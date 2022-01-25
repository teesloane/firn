use crate::{
    config::{BaseUrl, Config},
    errors::{FirnError, FirnErrorType},
    front_matter,
    html::{self, MyHtmlHandler},
    org::OrgFile,
    user_config,
};
use orgize::export::HtmlHandler;
use orgize::{Element, Event, Org, ParseConfig};
use serde_json::value::{from_value, to_value, Value};
use std::collections::HashMap;
use std::path::PathBuf;
use tera::{Function as TeraFn, Result as TeraResult};

#[derive(Debug, Clone)]
pub struct Render {
    original_org: String,
    base_url: BaseUrl,
    file_path: PathBuf,
    front_matter: front_matter::FrontMatter,
    verbosity: u8,
    user_config: user_config::UserConfig,
}

// Regarding transforming orgize into html:
// We start at
// -> org.rs
// -> render template fn (where links get transformed for example, with baseurl)
// -> MyHTMLHandler (html.rs).

impl Render {
    pub fn new(o: &OrgFile, cfg: &Config) -> Render {
        Render {
            original_org: o.original_org.clone(),
            front_matter: o.front_matter.clone(),
            file_path: o.file_path.clone(),
            base_url: cfg.base_url.clone(),
            verbosity: cfg.verbosity,
            user_config: cfg.user_config.clone(),
        }
    }

    pub fn render_entire_file(&self, update_level: Option<i8>) -> String {
        let parsed = Org::parse_custom(
            &self.original_org,
            &ParseConfig {
                todo_keywords: (self.user_config.file.todo_keywords.clone(), vec![]),
            },
        );
        let mut wr = Vec::new();
        let mut handler = MyHtmlHandler::default();

        // here we manually write out html *only when we are in the headlines we want*.
        for event in parsed.iter() {
            match event {
                Event::Start(el) => match el {
                    Element::Title(title) => {
                        html::write_title(title, &mut handler, &mut wr, update_level)
                    }
                    Element::Link(link) => html::write_link(
                        link,
                        &mut handler,
                        &mut wr,
                        self.base_url.clone(),
                        self.file_path.clone(),
                    ),
                    _ => handler.start(&mut wr, el).unwrap(),
                },
                Event::End(el) => handler.end(&mut wr, el).unwrap(),
            }
        }
        String::from_utf8(wr).unwrap()
    }

    pub fn render_headline(
        &self,
        headline: &str,
        update_level: Option<i8>,
    ) -> Result<String, FirnError> {
        let parsed = Org::parse_custom(
            &self.original_org,
            &ParseConfig {
                todo_keywords: (self.user_config.file.todo_keywords.clone(), vec![]),
            },
        );
        let mut wr = Vec::new();
        let mut handler = MyHtmlHandler::default();
        let mut is_writing = false;
        let mut headline_found = false;
        let mut is_writing_level = 0;

        // here we manually write out html *only when we are in the headlines we want*.
        for event in parsed.iter() {
            match event {
                Event::Start(el) => {
                    if is_writing {
                        match el {
                            Element::Title(title) => {
                                html::write_title(title, &mut handler, &mut wr, update_level)
                            }
                            Element::Link(link) => html::write_link(
                                link,
                                &mut handler,
                                &mut wr,
                                self.base_url.clone(),
                                self.file_path.clone(),
                            ),
                            _ => handler.start(&mut wr, el).unwrap(),
                        }
                    }
                }

                Event::End(el) => {
                    if is_writing {
                        handler.end(&mut wr, el).unwrap();
                    } else {
                        // A bit hacky, but we meet titles when iterating like so:
                        //  // Start(Text { value: "Notes" })
                        //  // End(Text { value: "Notes" }) // <<< this is the problem
                        //  // End(Title(Title { level: 1, priority: None, tags: [], keyword: None, raw: "Notes", planning: None, properties: {}, post_blank: 1 }))
                        //  so, we only actually want to start "capturing" (ie: is_writing), when we've already "passed" the "Title > text > Title" group.
                        if let Element::Title(title) = el {
                            if headline == title.raw {
                                headline_found = true;
                                is_writing = true;
                                is_writing_level = title.level;
                            }
                        }
                    }
                    if let Element::Headline { level } = el {
                        if is_writing && &is_writing_level == level {
                            handler.end(&mut wr, el).unwrap();
                            is_writing = false;
                            is_writing_level = 0;
                        }
                    }
                }
            }
        }

        if !headline_found {
            return Err(FirnError::new(
                &format!("No headline found for {:?}", &self.front_matter.title),
                FirnErrorType::HeadlineNotFound,
            ));
        } else {
            let as_html = String::from_utf8(wr).unwrap();
            Ok(as_html)
        }
    }
}

impl<'a> TeraFn for Render {
    fn call(&self, args: &HashMap<String, Value>) -> TeraResult<Value> {
        let headline = optional_arg!(
            String,
            args.get("headline"),
            "`render` requires `headline` to be a String"
        );

        let update_level = optional_arg!(
            i8,
            args.get("update_level"),
            "`render` requires `update_level` to be an int"
        );

        // If we have a headline, try and find it in the file and return it's contents.
        if let Some(headline) = headline {
            match self.render_headline(&headline, update_level) {
                Ok(headline_contents) => Ok(to_value(headline_contents).unwrap()),
                Err(_e) => {
                    if self.verbosity == 1 {
                        println!("\n⚠️ Warning: Firn could not find the headline {:?}\nfor file {:?};\nThe layout ({:?}), which is trying to render the headline will be partially empty.",
                                 headline, self.file_path, self.front_matter.layout.as_deref().unwrap_or("")
                        );
                    }
                    Ok(tera::Value::String("".to_string()))
                }
            }
        } else {
            // no headline? Return the entire file rendered
            Ok(to_value(&self.render_entire_file(update_level)).unwrap())
        }
    }
}
