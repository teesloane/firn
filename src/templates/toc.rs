use crate::org::OrgFile;
use crate::{user_config, util};
use orgize::export::{DefaultHtmlHandler, HtmlEscape, HtmlHandler};
use orgize::Element;
use orgize::{Org, ParseConfig};
use serde_json::value::{to_value, Value};
use std::collections::HashMap;
use std::io::Error as IOError;
use std::io::Write;
use std::string::FromUtf8Error;
use tera::{from_value, Function as TeraFn, Result as TeraResult};

pub struct Toc {
    original_org: String,
    user_config: user_config::UserConfig,
}

impl Toc {
    pub fn new(o: &OrgFile, user_config: user_config::UserConfig) -> Toc {
        Toc {
            original_org: o.original_org.clone(),
            user_config,
        }
    }

    /// Takes the parsed org mode, fetches the headlines, and selects to return
    /// some html based on user input requirements.
    /// Iterates over the parsed headlines and returns a table of contents string.
    /// NOTE: This currently works but it is not 100% effective - it does not handle cases
    /// where a headline section (ex: h1) skips straight to (h3); ie, it does not render
    /// two indents forward in the toc - but only one.
    pub fn create_toc(
        &self,
        list_type: Option<String>,
        depth: Option<usize>,
        headline_start: Option<String>,
        exclude_root: Option<bool>,
    ) -> String {
        let parsed = Org::parse_custom(
            &self.original_org,
            &ParseConfig {
                todo_keywords: (self.user_config.file.todo_keywords.clone(), vec![]),
            },
        );

        let mut writer = Vec::new();
        let mut prev_headline_level = 0;
        let mut at_headline_root = false;
        let mut headline_root_lvl = 0;
        let list_type = list_type.unwrap_or("ol".to_string());
        let exclude_root = exclude_root.unwrap_or(false);
        for headline in parsed.headlines() {
            let title = headline.title(&parsed);
            let hl_lvl = headline.level();
            let user_preferred_depth = depth.unwrap_or(6);

            // a fn closure for the act of actually writing the html
            let mut write_headline = || {
                // if the current hl_lvl we are iterating on is less than the depth..
                if hl_lvl <= user_preferred_depth {


                    // and it's more than the prev headline...
                    if hl_lvl > prev_headline_level {
                        // let's open a new ul/ol tag for it.
                        write!(writer, "<{0}>", list_type)
                            .expect("Failed to use writer in toc function.")
                    }
                    // if a headline jumps from say *5* in depth to *2*,
                    // we need to write that many (3) closing ul/ol tags.
                    if hl_lvl < prev_headline_level {
                        let gap_between_headlines = prev_headline_level - hl_lvl;
                        for _ in 0..gap_between_headlines {
                            write!(writer, "</{0}>", list_type)
                                .expect("Failed to use writer in toc function.");

                        }
                    }

                    // parse just the raw headline.
                    let mut headline_writer = Vec::new();
                    // we use a custom html hander that does the html writing.
                    let mut toc_handler = TocHtmlHandler::default();
                    Org::parse(&title.raw)
                        .write_html_custom(&mut headline_writer, &mut toc_handler)
                        .unwrap();
                    let headline_html_str = String::from_utf8(headline_writer).unwrap();
                    // now we can actually write a list item with the entry.
                    write!(
                        writer,
                        "<li><a href=\"#{0}\">{1}</a></li>",
                        &title.raw, headline_html_str
                    )
                    .expect("Failed to write table of contents");
                prev_headline_level = hl_lvl;
                // ?????
                } else {
                    prev_headline_level = hl_lvl;
                }
            };

            // If we have specified a headline to start at,
            // then write the headlines when we are only under that node.
            if let Some(headline_start) = &headline_start {
                let title = title.raw.clone().to_string();
                let headline_start = headline_start.clone();
                // we have found the headline specified.
                if title == headline_start {
                    at_headline_root = true;
                    headline_root_lvl = hl_lvl;
                    if !exclude_root {
                        write_headline();
                    }
                }

                if hl_lvl > headline_root_lvl && at_headline_root {
                    write_headline();
                }

                if hl_lvl == headline_root_lvl && title != headline_start {
                    at_headline_root = false;
                }
            } else {
                write_headline();
            }
        }
        write!(writer, "</{0}>", list_type).expect("Failed to write list_type closing");
        String::from_utf8(writer).unwrap()
    }
}

impl TeraFn for Toc {
    fn call(&self, args: &HashMap<String, Value>) -> TeraResult<Value> {
        let depth = optional_arg!(
            usize,
            args.get("depth"),
            "`toc` requires `depth` to be a non-negative integer"
        );

        let headline_start = optional_arg!(
            String,
            args.get("headline"),
            "`toc` requires `headline` to be a String"
        );

        let exclude_root = optional_arg!(
            bool,
            args.get("exclude_root"),
            "`toc` requires `exclude_root` to be a boolean"
        );

        let list_type = optional_arg!(
            String,
            args.get("list_type"),
            "`toc` requires `list_type` to be a 'ol' or 'ul'"
        );

        // validate it's an ol or a ul.
        if let Some(list_type) = &list_type {
            if list_type != "ol" && list_type != "ul" {
                println!("Error: toc(): \n`list_type` must be either `ol` or `ul`");
                util::exit();
            }
        }

        Ok(to_value(&self.create_toc(list_type, depth, headline_start, exclude_root)).unwrap())
    }
}

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

/// We have to have a very custom html handler for the table of contents
/// to stop from rendering tons of html content in the toc.
#[derive(Default)]
pub struct TocHtmlHandler(DefaultHtmlHandler);

impl HtmlHandler<MyError> for TocHtmlHandler {
    fn start<W: Write>(&mut self, mut w: W, element: &Element) -> Result<(), MyError> {
        match element {
            Element::Text { value } => write!(w, "{}", HtmlEscape(value))?,
            Element::Bold => write!(w, "<b>")?,
            Element::Strike => write!(w, "<s>")?,
            Element::Link(link) => write!(
                w,
                "<span>{}</span>",
                HtmlEscape(link.desc.as_ref().unwrap_or(&link.path)),
            )?,
            Element::Underline => write!(w, "<u>")?,
            Element::Verbatim { value } => write!(&mut w, "<code>{}</code>", HtmlEscape(value))?,
            Element::Code { value } => write!(w, "<code>{}</code>", HtmlEscape(value))?,
            _ => (),
        }

        Ok(())
    }

    fn end<W: Write>(&mut self, mut w: W, element: &Element) -> Result<(), MyError> {
        match element {
            Element::Section => (),

            Element::Italic => write!(w, "</i>")?,
            Element::Strike => write!(w, "</s>")?,
            Element::Underline => write!(w, "</u>")?,
            Element::Bold => write!(w, "</b>")?,
            Element::InlineSrc(inline_src) => write!(
                w,
                "<code class=\"src src-{}\">{}</code>",
                inline_src.lang,
                HtmlEscape(&inline_src.body)
            )?,
            Element::Keyword(_) => (),
            Element::Macros(_) => (),
            Element::Snippet(_) => (),
            _ => (),
        }

        Ok(())
    }
}
