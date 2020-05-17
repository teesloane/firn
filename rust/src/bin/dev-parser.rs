// This is the script to compile a bin version of the dev parser, rather than
// the jni version.

use orgize::Org;
use serde_json::{to_string};
use std::env;


fn main() {
    let args: Vec<String> = env::args().collect();
    let input = &args[1];
    let org = Org::parse(input);
    println!("{}", to_string(&org).unwrap());
}
