use orgize::export::{DefaultHtmlHandler, HtmlEscape, HtmlHandler};
use orgize::{elements, Element};
use std::io::{Error as IOError, Write};
use std::string::FromUtf8Error;
use crate::templates::links;
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

// this handles the actual writing of html.
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


// -- HTML Transformers
//
// Before we hand off orgize-data structures to the above html handler for writing html
// we sometimes need to transform the data based on user customization / help etc.
// most of these need to just transfer the beginning of the html (the start fn needed by the trait.)

pub fn trx_link(
    link: &elements::Link,
    handler: &mut MyHtmlHandler,
    writer: &mut Vec<u8>,
    base_url: String,
) {
    let link_web_path = links::link_transformer(base_url, link.path.to_owned().to_string());
    let new_link = elements::Link {
        path: std::borrow::Cow::Borrowed(&link_web_path),
        desc: link.desc.to_owned(),
    };
    let new_link_enum = Element::Link(new_link);
     handler.start(writer, &new_link_enum).unwrap()
}


pub fn trx_title(
    title: &elements::Title,
    handler: &mut MyHtmlHandler,
    writer: &mut Vec<u8>,
    update_level: Option<i8>
) {
    let update_level = update_level.unwrap_or(0);
    let mut new_level = title.level as i8 + update_level;
    if new_level > 6 {
        new_level = 6
    } else if new_level < 1 {
        new_level = 1
    }

    let new_title_inner = elements::Title {
        level: new_level as usize,
        ..title.to_owned()
    };
    let new_title = Element::Title(new_title_inner);
    handler.start(writer, &new_title).unwrap()
}
