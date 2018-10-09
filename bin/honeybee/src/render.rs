/*
 * Copyright (C) 2018  Minnesota Department of Transportation
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */
use multi::*;
use raster::Raster;

/// Value result from parsing MULTI.
type UnitResult = Result<(), SyntaxError>;

/// Text render state
#[derive(Copy,Clone)]
pub struct RenderState {
    color_scheme    : ColorScheme,
    color_foreground: Color,
    color_background: Color,    // background color of text
    page_background : Color,
    page_on_time_ds : u8,       // deciseconds
    page_off_time_ds: u8,       // deciseconds
    text_rectangle  : Rectangle,
    just_page       : PageJustification,
    just_line       : LineJustification,
    line_spacing    : Option<u8>,
    char_spacing    : Option<u8>,
    char_width      : u8,
    char_height     : u8,
    font            : (u8, Option<u16>),
}

/// Page splitter (iterator)
pub struct PageSplitter<'a> {
    default_state   : RenderState,
    render_state    : RenderState,
    parser          : Parser<'a>,
    more            : bool,
}

/// Page renderer
pub struct PageRenderer {
    render_state    : RenderState,
    values          : Vec<Value>,
}

impl RenderState {
    /// Create a new render state.
    pub fn new(color_scheme     : ColorScheme,
               color_foreground : Color,
               page_background  : Color,
               page_on_time_ds  : u8,
               page_off_time_ds : u8,
               text_rectangle   : Rectangle,
               just_page        : PageJustification,
               just_line        : LineJustification,
               char_width       : u8,
               char_height      : u8,
               font             : (u8, Option<u16>)) -> Self
    {
        let color_background = page_background;
        RenderState {
            color_scheme,
            color_foreground,
            color_background,
            page_background,
            page_on_time_ds,
            page_off_time_ds,
            text_rectangle,
            line_spacing : None,
            char_spacing : None,
            just_page,
            just_line,
            char_width,
            char_height,
            font,
        }
    }
    /// Check if the sign is a character-matrix.
    fn is_char_matrix(&self) -> bool {
        self.char_width > 0
    }
    /// Check if the sign is a full-matrix.
    fn is_full_matrix(&self) -> bool {
        self.char_width == 0 && self.char_height == 0
    }
    /// Get the character width (1 for variable width).
    fn char_width(&self) -> u32 {
        if self.is_char_matrix() {
            self.char_width as u32
        } else {
            1
        }
    }
    /// Get the character height (1 for variable height).
    fn char_height(&self) -> u32 {
        if self.char_height > 0 {
            self.char_height as u32
        } else {
            1
        }
    }
    /// Update the render state with a MULTI value.
    ///
    /// * `default_state` Default render state.
    /// * `v` MULTI value.
    fn update(&mut self, default_state: &RenderState, v: &Value) -> UnitResult {
        match v {
            Value::ColorBackground(None) => {
                self.color_background = default_state.color_background;
            },
            Value::ColorBackground(Some(c)) => { self.color_background = *c },
            Value::ColorForeground(None) => {
                self.color_foreground = default_state.color_foreground;
            },
            Value::ColorForeground(Some(c)) => { self.color_foreground = *c },
            Value::Font(None) => { self.font = default_state.font },
            Value::Font(Some(f)) => { self.font = *f },
            Value::JustificationLine(jl) => {
                self.just_line = jl.unwrap_or(default_state.just_line);
            },
            Value::JustificationPage(jp) => {
                self.just_page = jp.unwrap_or(default_state.just_page);
            },
            Value::NewLine(None) => { self.line_spacing = None; },
            Value::NewLine(Some(ls)) => {
                if !self.is_full_matrix() {
                    return Err(SyntaxError::UnsupportedTagValue);
                }
                self.line_spacing = Some(*ls);
            },
            Value::PageBackground(None) => {
                self.page_background = default_state.page_background;
            },
            Value::PageBackground(Some(c)) => { self.page_background = *c; },
            Value::PageTime(on, off) => {
                self.page_on_time_ds = on.unwrap_or(
                    default_state.page_on_time_ds
                );
                self.page_off_time_ds = off.unwrap_or(
                    default_state.page_off_time_ds
                );
            },
            Value::SpacingCharacter(sc) => {
                if self.is_char_matrix() {
                    return Err(SyntaxError::UnsupportedTag("sc".to_string()));
                }
                self.char_spacing = Some(*sc);
            },
            Value::SpacingCharacterEnd() => { self.char_spacing = None; },
            Value::TextRectangle(r) => {
                return self.update_text_rectangle(default_state, r);
            },
            Value::Text(_) => (),
            _ => {
                // Unsupported tags: [f], [fl], [hc], [ms], [mv]
                return Err(SyntaxError::UnsupportedTag(v.to_string()));
            },
        }
        Ok(())
    }
    fn update_text_rectangle(&mut self, default_state: &RenderState,
        r: &Rectangle) -> UnitResult
    {
        if !default_state.text_rectangle.contains(r) {
            return Err(SyntaxError::UnsupportedTagValue);
        }
        let cw = self.char_width as u16;
        if cw > 0 {
            // Check text rectangle matches character boundaries
            let x = r.x - 1;
            if x % cw != 0 || r.w % cw != 0 {
                return Err(SyntaxError::UnsupportedTagValue);
            }
        }
        let lh = self.char_height as u16;
        if lh > 0 {
            // Check text rectangle matches line boundaries
            let y = r.y - 1;
            if y % lh != 0 || r.h % lh != 0 {
                return Err(SyntaxError::UnsupportedTagValue);
            }
        }
        self.text_rectangle = *r;
        Ok(())
    }
}

/*
impl<'a> Span<'a> {
    fn new(s: String, rs: RenderState) -> Self {
        Span { span: s, render_state: rs }
    }
    fn char_spacing(&self) -> u8 {
        let rs = self.render_state;
        match rs.char_spacing {
            Some(cs) => cs,
            _        => rs.font.char_spacing(),
        }
    }
    fn char_spacing_avg(&self, other: &Self) -> u8 {
        let sp0 = self.char_spacing();
        let sp1 = other.char_spacing();
        // NTCIP 1203 fontCharSpacing:
        // "... the average character spacing of the two fonts,
        // rounded up to the nearest whole pixel ..." ???
        ((sp0 + sp1) as f32 / 2f32).round() as u8
    }
    fn width(&self) -> u32 {
        let span = self.span;
        let cs = self.char_spacing();
        self.render_state.font.width(span, cs)
    }
    fn height(&self) -> u32 {
        self.render_state.font.height()
    }
    fn line_spacing(&self) -> u8 {
        let rs = self.render_state;
        match rs.line_spacing {
            Some(ls) => ls,
            _        => rs.font.line_spacing(),
        }
    }
    fn render(&mut self, raster: &mut Raster, left: u32, base: u32)
        -> UnitResult
    {
        let mut x = left;
        let y = base - self.height();
        let cs = self.char_spacing();
        let fg = self.render_state.color_foreground;
        for cp in self.span.chars() {
            let g = self.render_state.font.get_char(cp)?;
            raster.render_graphic(g, fg, x, y);
            x += g.width() + cs;
        }
        Ok(())
    }
}*/
/*
impl<'a> Fragment<'a> {
    fn new(rs: RenderState) -> Self {
        Fragment {
            spans: vec!(),
            render_state: rs,
        }
    }
    fn height(&self) -> u32 {
        match self.spans.iter().map(|s| s.height()).max() {
            Some(h) => h,
            _       => 0,
        }
    }
    fn line_spacing(&self) -> u8 {
        match self.spans.iter().map(|s| s.line_spacing()).max() {
            Some(s) => s,
            _       => 0,
        }
    }
    fn add_span(&mut self, s: String) {
        let rs = self.render_state;
        self.spans.push(Span::new(s, rs));
    }
    fn render(&self, raster: &mut Raster, base: u32) -> UnitResult {
        let mut x = self.left()?;
        let pspan = None;
        for span in self.spans {
            if let Some(ps) = pspan {
                x += span.char_spacing_avg(ps);
            }
            span.render(raster, x, base)?;
            x += span.width();
            pspan = Some(&span);
        }
        Ok(())
    }
    fn left(&self) -> Result<u32, SyntaxError> {
        let ex = self.extra_width()?;
        let jl = self.render_state.just_line;
        let x = self.render_state.text_rectangle.x;
        match jl {
            // FIXME: add LineJustification::Full
            LineJustification::Left   => Ok(x),
            LineJustification::Center => Ok(x + self.char_width_floor(ex / 2)),
            LineJustification::Right  => Ok(x + ex),
            _                         => Err(SyntaxError::UnsupportedTagValue),
        }
    }
    fn extra_width(&self) -> Result<u32, SyntaxError> {
        let pw = self.render_state.text_rectangle.w;
        let tw = self.width();
        let cw = self.render_state.char_width();
        let w = pw / cw;
        let r = tw / cw;
        if w >= r {
            Ok((w - r) * cw)
        } else {
            Err(SyntaxError::TextTooBig)
        }
    }
    fn char_width_floor(&self, ex: u32) -> u32 {
        let cw = self.render_state.char_width();
        (ex / cw) * cw
    }
    fn width(&self) -> u32 {
        let mut w = 0;
        let pspan = None;
        for span in self.spans {
            let sw = span.width();
            if let Some(ps) = pspan {
                if sw > 0 {
                    w += sw + span.char_spacing_avg(ps);
                    pspan = Some(&span);
                }
            }
        }
        w
    }
}*/
/*
impl<'a> Line<'a> {
    fn new(render_state: RenderState) -> Self {
        Line { fragments: vec!(), render_state }
    }
    fn height(&self) -> u32 {
        match self.fragments.iter().map(|f| f.height()).max() {
            Some(h) => h,
            _       => 0,
        }
    }
    fn line_spacing(&self) -> u8 {
        match self.fragments.iter().map(|f| f.line_spacing()).max() {
            Some(s) => s,
            _       => 0,
        }
    }
    fn line_spacing_avg(&self, other: &Self) -> u32 {
        let ls = self.render_state.line_spacing;
        match ls {
            Some(ls) => ls,
            _        => self.line_spacing_avg2(other),
        }
    }
    fn line_spacing_avg2(&self, other: &Self) -> u8 {
        let sp0 = self.line_spacing();
        let sp1 = other.line_spacing();
        // NTCIP 1203 fontLineSpacing:
        // "The number of pixels between adjacent lines
        // is the average of the 2 line spacings of each
        // line, rounded up to the nearest whole pixel."
        ((sp0 + sp1) as f32 / 2f32).round() as u32
    }
    fn last_fragment(&mut self) -> &mut Fragment<'a> {
        let len = self.fragments.len();
        if len == 0 {
            let rs = self.render_state;
            self.add_fragment(rs);
        }
        &mut self.fragments[len - 1]
    }
    fn add_span(&mut self, s: String) {
        self.last_fragment().add_span(s);
    }
    fn add_fragment(&mut self, rs: RenderState) {
        let f = Fragment::new(rs);
        self.fragments.push(f);
    }
    fn justification_line_used(&self) -> LineJustification {
        let len = self.fragments.len();
        if len > 0 {
            self.fragments[len - 1].render_state.just_line
        } else {
            LineJustification::Other
        }
    }
    fn render(&mut self, raster: &mut Raster, base: u32) -> UnitResult {
        for f in self.fragments {
            f.render(raster, base)?;
        }
        Ok(())
    }
}*/
/*
impl<'a> Block<'a> {
    fn new(render_state: RenderState) -> Block<'a> {
        Block { lines: vec!(), render_state }
    }
    fn add_span(&mut self, s: String) {
        self.last_line().add_span(s);
    }
    fn add_fragment(&mut self, rs: RenderState) {
        self.last_line().add_fragment(rs);
    }
    fn last_line(&mut self) -> &mut Line<'a> {
        let len = self.lines.len();
        if len == 0 {
            let line = Line::new(self.render_state);
            self.lines.push(line);
        }
        &mut self.lines[len - 1]
    }
    fn justification_line_used(&self) -> LineJustification {
        let len = self.lines.len();
        if len > 0 {
            self.lines[len - 1].justification_line_used()
        } else {
            LineJustification::Other
        }
    }
    fn add_line(&mut self, ls: Option<u32>) {
        let line = self.last_line();
        if line.height() == 0 {
            // The line height can be zero on full-matrix
            // signs when no text has been specified.
            // Adding an empty span to the line allows the
            // height to be taken from the current font.
            line.add_span("".to_string());
        }
        self.render_state.line_spacing = ls;
        let line = Line::new(self.render_state);
        self.lines.push(line);
    }
    fn render(&mut self, raster: &mut Raster) -> UnitResult {
        let top = self.top()?;
        let mut y = 0;
        let mut pline = None;
        for line in self.lines {
            if let Some(pl) = pline {
                y += line.line_spacing_avg(pl);
            }
            y += line.height();
            line.render(raster, top + y)?;
            pline = Some(&line);
        }
        Ok(())
    }
    fn top(&self) -> Result<u32, SyntaxError> {
        let ex = self.extra_height()?;
        let jp = self.render_state.just_page;
        let y = self.render_state.text_rectangle.y;
        match jp {
            PageJustification::Top    => Ok(y),
            PageJustification::Middle => Ok(y + self.char_height_floor(ex / 2)),
            PageJustification::Bottom => Ok(y + ex),
            _                         => Err(SyntaxError::UnsupportedTagValue),
        }
    }
    fn extra_height(&self) -> Result<u32, SyntaxError> {
        let ph = self.render_state.text_rectangle.h;
        let ch = self.render_state.char_height();
        let h = ph / ch;
        let r = self.height() / ch;
        if h >= r {
            Ok((h - r) * ch)
        } else {
            Err(SyntaxError::TextTooBig)
        }
    }
    fn char_height_floor(&self, ex: u32) -> u32 {
        let ch = self.render_state.char_height();
        (ex / ch) * ch
    }
    fn height(&self) -> u32 {
        let mut h = 0;
        let pline = None;
        for line in self.lines {
            let lh = line.height();
            if let Some(pl) = pline {
                if lh > 0 {
                    h += lh + line.line_spacing_avg(pl);
                    pline = Some(&line);
                }
            }
        }
        h
    }
}*/
/*
impl Renderer {
    fn last_block(&mut self) -> &Block<'a> {
        let len = self.blocks.len();
        if len == 0 {
            self.add_block();
        }
        &self.blocks[len - 1]
    }
    fn add_block(&mut self) {
        let block = Block::new(self.render_state);
        self.blocks.push(block);
    }
    pub fn add_span(&mut self, s: String) {
        self.last_block().add_span(s);
    }
    pub fn add_line(&mut self, ls: Option<u32>) -> UnitResult {
        self.last_block().add_line(ls);
        Ok(())
    }
    pub fn add_page(&mut self) -> UnitResult {
        self.draw_text()?;
        self.reset_text_rectangle();
        Ok(())
    }
    pub fn set_color_background(&mut self, cb: Color) {
        self.render_state.color_background = cb;
    }
    pub fn set_color_foreground(&mut self, cf: Color) {
        self.render_state.color_foreground = cf;
    }
    pub fn add_color_rectangle(&mut self, r: Rectangle, clr: Color) {
        self.fill_rectangle(r, clr);
    }
    fn fill_rectangle(&mut self, r: Rectangle, clr: Color) {
        let x = r.x - 1;
        let y = r.y - 1;
        let w = r.w;
        let h = r.h;
        for yy in 0..h {
            for xx in 0..w {
                raster.set_pixel(x + xx, y + yy, clr);
            }
        }
    }
    pub fn set_text_rectangle(&mut self, r: Rectangle) -> UnitResult {
        self.draw_text()?;
        if self.default_state.text_rectangle.contains(&r) {
            self.render_state.text_rectangle = r;
            Ok(())
        } else {
            Err(SyntaxError::UnsupportedTagValue)
        }
    }
    pub fn draw_text(&mut self) -> UnitResult {
        for block in self.blocks {
            block.render();
        }
        self.blocks.clear();
        Ok(())
    }
    pub fn add_graphic(&mut self, g: &Raster, x: u32, y: u32) -> UnitResult {
        let c = self.render_state.color_foreground;
        self.render_graphic(g, c, x - 1, y - 1)
    }
    fn render_graphic(&mut self, g: &Raster, clr: Color, x: u32, y: u32)
        -> UnitResult
    {
        self.raster.copy(g, x, y, clr)
    }
}*/


impl PageRenderer {
    /// Create a new page renderer
    pub fn new(render_state: RenderState, values: Vec<Value>) -> Self {
        PageRenderer {
            render_state,
            values,
        }
    }
    /// Render the page.
    pub fn render(&self) -> Result<Raster, SyntaxError> {
        let w = self.render_state.text_rectangle.w;
        let h = self.render_state.text_rectangle.h;
        let clr = self.render_state.page_background.rgb(
            self.render_state.color_scheme);
        if clr.is_none() {
            return Err(SyntaxError::Other);
        }
        let clr = clr.unwrap();
        let rgba = [clr[0], clr[1], clr[2], 0];
        let mut page = Raster::new(w.into(), h.into(), rgba);
        let len = self.values.len();
/*        let mut rects = vec!();
        for i in 0..len {
            let v = self.values[i];
            // FIXME
        }*/
        Ok(page)
    }
}

impl<'a> PageSplitter<'a> {
    /// Create a new page splitter.
    ///
    /// * `render_state` Default render state.
    /// * `ms` MULTI string to parse.
    pub fn new(render_state: RenderState, ms: &'a str) -> Self {
        let parser = Parser::new(ms);
        let default_state = render_state;
        let more = true;
        PageSplitter { default_state, render_state, parser, more }
    }
    /// Make the next page.
    fn make_page(&mut self) -> Result<PageRenderer, SyntaxError> {
        self.more = false;
        let mut rs = self.page_state();
        let mut values = vec!();
        while let Some(t) = self.parser.next() {
            let v = t?;
            if let Value::NewPage() = v {
                self.more = true;
                break;
            }
            self.render_state.update(&self.default_state, &v)?;
            values.push(v);
        }
        // These values affect the entire page
        rs.page_background = self.render_state.page_background;
        rs.page_on_time_ds = self.render_state.page_on_time_ds;
        rs.page_off_time_ds = self.render_state.page_off_time_ds;
        Ok(PageRenderer::new(rs, values))
    }
    /// Get the current page state.
    fn page_state(&self) -> RenderState {
        let mut rs = self.render_state;
        // Set these back to default values
        rs.text_rectangle = self.default_state.text_rectangle;
        rs.line_spacing = self.default_state.line_spacing;
        rs
    }
}

impl<'a> Iterator for PageSplitter<'a> {
    type Item = Result<PageRenderer, SyntaxError>;

    fn next(&mut self) -> Option<Result<PageRenderer, SyntaxError>> {
        if self.more {
            Some(self.make_page())
        } else {
            None
        }
    }
}


// Layout algorithm:
//
// Vec of rectangles for block, line, fragment, span
//  [jp]  block
//  [nl]  line
//  [jl]  fragment
// (text) span



#[cfg(test)]
mod test {
    use super::*;
    fn make_full_matrix() -> RenderState {
        RenderState::new(ColorScheme::Monochrome1Bit,
                         Color::Legacy(1), Color::Legacy(0),
                         20, 0,
                         Rectangle::new(1, 1, 60, 30),
                         PageJustification::Top,
                         LineJustification::Left,
                         0, 0, (1, None))
    }
    #[test]
    fn page_count() {
        let rs = make_full_matrix();
        let pages: Vec<_> = PageSplitter::new(rs, "").collect();
        assert!(pages.len() == 1);
        let pages: Vec<_> = PageSplitter::new(rs, "1").collect();
        assert!(pages.len() == 1);
        let pages: Vec<_> = PageSplitter::new(rs, "[np]").collect();
        assert!(pages.len() == 2);
        let pages: Vec<_> = PageSplitter::new(rs, "1[NP]").collect();
        assert!(pages.len() == 2);
        let pages: Vec<_> = PageSplitter::new(rs, "1[Np]2").collect();
        assert!(pages.len() == 2);
        let pages: Vec<_> = PageSplitter::new(rs, "1[np]2[nP]").collect();
        assert!(pages.len() == 3);
    }
    #[test]
    fn page_full_matrix() {
        let rs = make_full_matrix();
        let mut pages = PageSplitter::new(rs, "");
        let p = pages.next().unwrap().unwrap();
        let rs = p.render_state;
        assert!(rs.color_scheme == ColorScheme::Monochrome1Bit);
        assert!(rs.color_foreground == Color::Legacy(1));
        assert!(rs.color_background == Color::Legacy(0));
        assert!(rs.page_background == Color::Legacy(0));
        assert!(rs.page_on_time_ds == 20);
        assert!(rs.page_off_time_ds == 0);
        assert!(rs.text_rectangle == Rectangle::new(1,1,60,30));
        assert!(rs.just_page == PageJustification::Top);
        assert!(rs.just_line == LineJustification::Left);
        assert!(rs.line_spacing == None);
        assert!(rs.char_spacing == None);
        assert!(rs.char_width == 0);
        assert!(rs.char_height == 0);
        assert!(rs.font == (1, None));
        let mut pages = PageSplitter::new(rs, "[pt10o2][cb9][pb5][cf3][jp3]\
            [jl4][tr1,1,10,10][nl4][fo3,1234][sc2][np][pb][pt][cb][/sc]");
        let p = pages.next().unwrap().unwrap();
        let rs = p.render_state;
        assert!(rs.color_foreground == Color::Legacy(1));
        assert!(rs.color_background == Color::Legacy(0));
        assert!(rs.page_background == Color::Legacy(5));
        assert!(rs.page_on_time_ds == 10);
        assert!(rs.page_off_time_ds == 2);
        assert!(rs.text_rectangle == Rectangle::new(1,1,60,30));
        assert!(rs.just_page == PageJustification::Top);
        assert!(rs.just_line == LineJustification::Left);
        assert!(rs.line_spacing == None);
        assert!(rs.char_spacing == None);
        assert!(rs.font == (1, None));
        let p = pages.next().unwrap().unwrap();
        let rs = p.render_state;
        assert!(rs.color_foreground == Color::Legacy(3));
        assert!(rs.color_background == Color::Legacy(9));
        assert!(rs.page_background == Color::Legacy(0));
        assert!(rs.page_on_time_ds == 20);
        assert!(rs.page_off_time_ds == 0);
        assert!(rs.text_rectangle == Rectangle::new(1,1,60,30));
        assert!(rs.just_page == PageJustification::Middle);
        assert!(rs.just_line == LineJustification::Right);
        assert!(rs.line_spacing == None);
        assert!(rs.char_spacing == Some(2));
        assert!(rs.font == (3, Some(0x1234)));
    }
    fn make_char_matrix() -> RenderState {
        RenderState::new(ColorScheme::Monochrome1Bit,
                         Color::Legacy(1), Color::Legacy(0),
                         20, 0,
                         Rectangle::new(1, 1, 100, 21),
                         PageJustification::Top,
                         LineJustification::Left,
                         5, 7, (1, None))
    }
    #[test]
    fn page_char_matrix() {
        let rs = make_char_matrix();
        let mut pages = PageSplitter::new(rs, "[tr1,1,12,12]");
        if let Some(Err(SyntaxError::UnsupportedTagValue)) = pages.next() {
            assert!(true);
        } else { assert!(false) }
        let mut pages = PageSplitter::new(rs, "[tr1,1,50,12]");
        if let Some(Err(SyntaxError::UnsupportedTagValue)) = pages.next() {
            assert!(true);
        } else { assert!(false) }
        let mut pages = PageSplitter::new(rs, "[tr1,1,12,14]");
        if let Some(Err(SyntaxError::UnsupportedTagValue)) = pages.next() {
            assert!(true);
        } else { assert!(false) }
        let mut pages = PageSplitter::new(rs, "[tr1,1,50,14]");
        if let Some(Ok(_)) = pages.next() { assert!(true); }
        else { assert!(false) }
    }
}