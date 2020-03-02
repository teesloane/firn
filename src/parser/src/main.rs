use orgize::Org;
use serde_json::{to_string};
use std::env;


fn main() {
    let args: Vec<String> = env::args().collect();
    let input = &args[1];
    let org = Org::parse(input);
    // let org = Org::parse("I 'm *bold*.");
    println!("{}", to_string(&org).unwrap());
}
