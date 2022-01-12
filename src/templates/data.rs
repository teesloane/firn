use crate::{
    org::{OrgMetadata, OrgTagType},
    util,
};
/// The data file is mostly for "serializing" larger internal structs that
/// aren't available to the user into smaller structs that get passed to Tera
/// These structs can be generally considered the "api" for the user, when they
/// are setting up their templates.
use serde::Serialize;

#[derive(Debug, PartialEq, Serialize)]
pub struct Backlink {
    path: String,
    file: String,
}

impl Backlink {
    pub fn new(path: String, file: String) -> Backlink {
        Backlink { path, file }
    }
}

#[derive(Debug, PartialEq, Serialize)]
/// Tag is a serialized version of the OrgMetadata struct
/// specifically for enabling the user to render tag data in templates.
pub struct Tag {
    // pub entity: OrgMetadataType<'a>,
    pub tag_type: String,
    pub title: String,
    pub path: String,
}

impl Tag {
    // I guess this could fail if I didn't pass in org metadata of type tag...
    pub fn new(om: OrgMetadata, baseurl: String) -> Tag {
        match om.entity {
            crate::org::OrgMetadataType::Tag(_tag_name, tag_type) => match tag_type {
                OrgTagType::FirnTag => {
                    let path = format!(
                        "{}/{}",
                        baseurl,
                        util::path_to_string(&om.originating_file_web_path)
                    );
                    Tag {
                        tag_type: "firn".to_string(),
                        title: om.originating_file,
                        path,
                    }
                }
                OrgTagType::OrgTag => {
                    let loc = om.originating_headline_web_path.expect(
                        "Internal error: OrgMetadata::FirnTag headline did not have a path",
                    );
                    let path = format!("{}/{}", baseurl, loc);
                    Tag {
                        tag_type: "org".to_string(),
                        title: om
                            .originating_headline
                            .expect("Internal error: OrgMetadata::FirnTag did not have a headline"),
                        path,
                    }
                }
            },
            _ => panic!(
                "Internal error: tried passing org metadata that
            wasn't of type 'tag' to creation of template Tag"
            ),
        }
    }
}
