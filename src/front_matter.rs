use crate::org::{OrgFileType, OrgMetadata, OrgMetadataType, OrgTagType};
use orgize::elements::PropertiesMap;
use serde::{Deserialize, Serialize};
use std::collections::HashMap;
use std::path::PathBuf;

#[derive(Serialize, Deserialize, Debug, Clone, PartialEq)]
pub struct FrontMatter {
    pub title: Option<String>,
    pub date_created: Option<String>,
    pub date_created_ts: Option<i64>,
    pub date_updated: Option<String>,
    pub date_updated_ts: Option<i64>,
    pub firn_under: Option<Vec<String>>,
    pub firn_tags: Option<Vec<String>>,
    pub firn_type: String,
    pub layout: Option<String>,
    pub firn_sitemap: bool,
    pub firn_private: bool,
    pub firn_properties: bool,
    pub file_type: Option<String>,
    pub other: HashMap<String, String>,
}

impl FrontMatter {
    pub fn default() -> FrontMatter {
        FrontMatter {
            title: None,
            date_created: None,
            date_created_ts: None,
            date_updated: None,
            date_updated_ts: None,
            firn_under: None,
            firn_tags: None,
            firn_type: "page".to_string(),
            layout: None,
            file_type: None,
            firn_sitemap: true,
            firn_private: false,
            firn_properties: false,
            other: HashMap::new(),
        }
    }

    pub fn new(parsed: &orgize::Org) -> FrontMatter {
        let mut fm = FrontMatter::default();
        fm.collect(parsed);
        fm
    }

    pub fn new_from_properties_map(properties: PropertiesMap) -> FrontMatter {
        // take the properties map, which has a pair field of tuples of cow strs,
        // transform them into a more reliable format (lowercase 'em)
        // then we can match over each one and construct frontmatter for it.
        let properties: Vec<(String, String)> = properties
            .pairs
            .iter()
            .map(|p| {
                (
                    p.0.to_lowercase().to_string(),
                    p.1.to_lowercase().to_string(),
                )
            })
            .collect();
        let mut fm = FrontMatter::default();
        for (k, v) in properties {
            fm.match_keyword(k, v);
        }
        // run: self.validate_frontmatter - fail if something is missing?
        fm

    }

    pub fn collect(&mut self, parsed: &orgize::Org) {
        for keyword in parsed.keywords() {
            let k = keyword.key.to_lowercase();
            let v = keyword.value.to_string();
            self.match_keyword(k, v);
        }
    }

    fn match_keyword(&mut self, k: String, v: String) {
        match &k[..] {
            "title" => {
                if v.is_empty() {
                    self.title = None
                } else {
                    self.title = Some(v)
                }
            }
            "date_created" => {
                if !v.is_empty() {
                    if let Some(dc) = FrontMatter::get_date_from_field(&v) {
                        self.date_created_ts = Some(dc.timestamp());
                        self.date_created = Some(dc.format("%Y-%m-%d").to_string());
                    }
                }
            }
            "date_updated" => {
                if !v.is_empty() {
                    if let Some(du) = FrontMatter::get_date_from_field(&v) {
                        self.date_updated_ts = Some(du.timestamp());
                        self.date_updated = Some(du.format("%Y-%m-%d").to_string());
                    }
                }
            }
            "firn_layout" => self.layout = Some(v),
            "firn_type" => {
                if v == "post" {
                    self.firn_type = "post".to_string();
                } else {
                    self.firn_type = "page".to_string();
                }
            }
            "firn_under" => {
                self.firn_under = Some(str_to_vec(v));
            }
            "firn_tags" => self.firn_tags = Some(str_to_vec(v)),
            "roam_tags" => self.firn_tags = Some(str_to_vec(v)),
            // NOTE: If a boolean based keyword is present at all, that is sufficient to say that it is true
            "firn_private" => self.firn_private = true,
            "firn_sitemap" => self.firn_sitemap = v.parse().unwrap_or(true),
            "firn_properties" => self.firn_properties = true,
            _ => {
                self.other.insert(k, v);
            }
        }
    }

    /// we get front matter via parsed_org.keywords() above.
    /// however, all the keyword *values* are not parsed - they are just raw strings.
    /// so, we have to re-parse their values. This function is responsible for parsing
    /// just values that are supposed to be orgize:DateTimes (#+date_created, #+date_updated)
    fn get_date_from_field(val: &String) -> Option<chrono::NaiveDateTime> {
        if !val.is_empty() {
            let parsed_date = orgize::Org::parse(val);
            for p in parsed_date.iter() {
                if let orgize::Event::Start(el) = p {
                    if let orgize::Element::Timestamp(t) = el {
                        match t {
                            orgize::elements::Timestamp::Active {
                                start,
                                repeater: _,
                                delay: _,
                            } => {
                                let res: chrono::NaiveDateTime = start.into();
                                return Some(res);
                            }
                            _ => (),
                        }
                    }
                }
            }
        }
        None
    }

    pub fn get_layout(&self) -> String {
        if let Some(layout) = &self.layout {
            return layout.to_string();
        }
        "default".to_string()
    }

    pub fn get_title(&self) -> String {
        self.title
            .clone()
            .unwrap_or("< File Has No Title >".to_string())
    }

    pub fn is_public(&self) -> bool {
        !self.firn_private
    }

    pub fn is_private(&self) -> bool {
        self.firn_private
    }

    pub fn get_file_type(&self) -> OrgFileType {
        if let Some(file_type) = &self.file_type {
            if file_type == "post" {
                OrgFileType::FirnPost
            } else if file_type == "page" {
                OrgFileType::FirnPage
            } else {
                OrgFileType::FirnPage
            }
        } else {
            OrgFileType::FirnPage
        }
    }

    pub fn is_post(&self) -> bool {
        &self.firn_type == "post"
    }

    /// Converts firn_tags in front matter to OrgMetadata
    pub fn firn_link_to_org_metadata(
        &self,
        web_path: &PathBuf,
        file_path: &PathBuf,
        tags: &mut Vec<OrgMetadata>,
    ) {
        if let Some(firn_tags) = self.firn_tags.clone() {
            for ftag in firn_tags {
                let e = OrgMetadata::new(
                    OrgMetadataType::Tag(ftag, OrgTagType::FirnTag),
                    None,
                    web_path,
                    file_path,
                    &self.clone(),
                );
                tags.push(e);
            }
        }
    }
}

fn str_to_vec(val: String) -> Vec<String> {
    val.split_whitespace().map(|s| s.to_string()).collect()
}
