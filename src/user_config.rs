use serde::{Deserialize, Serialize};

use crate::util;

#[derive(Debug, Serialize, Deserialize, Clone)]
pub struct SiteConfig {
    pub url: String,
    pub title: String,
    pub description: String,
    pub ignored_directories: Vec<String>,
    pub data_directory: String,
    pub clean_attachments: bool,
    pub sass: String,
}

#[derive(Debug, Serialize, Deserialize, Clone)]
pub struct TagConfig {
    pub url: String,
    pub org: bool,
    pub firn: bool,
}

#[derive(Debug, Serialize, Deserialize, Clone)]
pub struct FileConfig {
    pub table_of_contents: String,
    pub todo_keywords: Vec<String>,
}

#[derive(Debug, Serialize, Deserialize, Clone)]
pub struct UserConfig {
    pub site: SiteConfig,
    pub file: FileConfig,
    pub tags: TagConfig,
}

impl UserConfig {
    pub fn get_tag_url(&self) -> String {
        if self.tags.url.is_empty() {
            println!("Error in configuration file: tags url field cannot be an empty string.");
            util::exit()
        } else if !self.tags.url.ends_with('/') {
            println!("Error in configuration file: tags > url field must end with `/`.");
            util::exit()
        } else {
            self.tags.url.clone()
        }
    }

    pub fn validate(&self) {
        if self.site.sass != "scss" && self.site.sass != "sass" {
            println!("Error in config.yaml: site > sass must be of value 'sass' or 'scss'");
            util::exit();
        }
    }
}
