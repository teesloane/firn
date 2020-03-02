use orgize::Org;
use serde_json::{to_string};


fn main() {
    let org = Org::parse("I 'm *bold*.");
    println!("{}", to_string(&org).unwrap());
}
