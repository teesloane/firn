use orgize::export::{DefaultHtmlHandler, HtmlEscape, HtmlHandler};
use orgize::Element;
use std::io::{Error as IOError, Write};
use std::string::FromUtf8Error;

use crate::util;

// -- HTML Handlers for Orgize -------------------------------------------------

// -- Here's we implement the custom html handler for Firn
// This allows us to attach classes to elements manually if need be.

#[derive(Debug)]
pub enum MyError {
    IO(IOError),
    Heading,
    Utf8(FromUtf8Error),
}

// From<std::io::Error> trait is required for custom error type
impl From<IOError> for MyError {
    fn from(err: IOError) -> Self {
        MyError::IO(err)
    }
}

impl From<FromUtf8Error> for MyError {
    fn from(err: FromUtf8Error) -> Self {
        MyError::Utf8(err)
    }
}

#[derive(Default)]
pub struct MyHtmlHandler(DefaultHtmlHandler);

impl HtmlHandler<MyError> for MyHtmlHandler {
    fn start<W: Write>(&mut self, mut w: W, element: &Element) -> Result<(), MyError> {
        match element {

            Element::Document { .. } => write!(w, "<div>")?,
            Element::Title(title) => {
                if title.level > 6 {
                    if let Some(keyword) = &title.keyword {
                        write!(w, "<h6 class=\"firn-{1}\" id=\"{0}\">", &title.raw, keyword)?;
                    } else {
                        write!(w, "<h6 id=\"{0}\">", &title.raw)?;
                    }
                } else if let Some(keyword) = &title.keyword {
                    write!(w, "<h{0} class=\"firn-{2}\" id=\"{1}\">", title.level, &title.raw, keyword)?;
                } else {
                    write!(w, "<h{0} id=\"{1}\">", title.level, &title.raw,)?;
                }
            }
            Element::Link(link) => {
                let link_path = &link.path.to_owned().to_string();
                if util::org_str_is_img_link(link_path) {
                    write!(w, "<img src=\"{}\"/>", HtmlEscape(&link.path))?
                } else {
                    write!(
                        w,
                        "<a href=\"{}\">{}</a>",
                        HtmlEscape(&link.path),
                        HtmlEscape(link.desc.as_ref().unwrap_or(&link.path)),
                    )?
                }
            }
            _ => self.0.start(w, element)?,
        }

        Ok(())
    }

    fn end<W: Write>(&mut self, mut w: W, element: &Element) -> Result<(), MyError> {
        match element {
            Element::Document { .. } => write!(w, "</div>")?,
            Element::Title(title) => {
                write!(w, "</h{}>", title.level)?;
            }
            _ => self.0.end(w, element)?,
        }

        Ok(())
    }
}
