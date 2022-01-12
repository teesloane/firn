use std::error::Error;
use std::fmt;

// borrowed from: https://stevedonovan.github.io/rust-gentle-intro/6-error-handling.html
#[derive(Debug, PartialEq, Eq, Hash, Clone)]
pub enum FirnErrorType {
    FrontMatterNoTitle,
    NoDefaultTemplateFound,
    IsPrivateFile,
    HeadlineNotFound
}

#[derive(Debug, PartialEq, Eq, Hash, Clone)]
pub struct FirnError {
    details: String,
    pub kind: FirnErrorType,
}

impl FirnError {
    pub fn new(msg: &str, kind: FirnErrorType) -> FirnError {
        FirnError {
            details: msg.to_string(),
            kind,
        }
    }

    pub fn get_err_name(ft: FirnErrorType) -> &'static str {
        match ft {
            FirnErrorType::FrontMatterNoTitle => "Missing `#+Title` frontmatter",
            FirnErrorType::NoDefaultTemplateFound => "No default.hbs template file found.", // it should just panic if this happens...
            FirnErrorType::IsPrivateFile => "File is private",
            FirnErrorType::HeadlineNotFound => "Headline not found",
        }
    }
}

impl fmt::Display for FirnError {
    fn fmt(&self, f: &mut fmt::Formatter) -> fmt::Result {
        write!(f, "{}", self.details)
    }
}

// NOTE: I think I should remove this stuff probably.
impl fmt::Display for FirnErrorType {
    fn fmt(&self, f: &mut fmt::Formatter) -> fmt::Result {
        match *self {
            FirnErrorType::FrontMatterNoTitle => write!(f, "No Title"),
            FirnErrorType::NoDefaultTemplateFound => write!(f, "No Template"),
            FirnErrorType::IsPrivateFile => write!(f, "File is private"),
            FirnErrorType::HeadlineNotFound => write!(f, "Headline not found."),
        }
    }
}

impl Error for FirnError {
    fn description(&self) -> &str {
        &self.details
    }
}
