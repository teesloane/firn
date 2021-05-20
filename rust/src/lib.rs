use orgize::{Org, ParseConfig};
use serde_json::to_string;
// This is the interface to the JVM that we'll call the majority of our
// methods on.
use jni::JNIEnv;

// These objects are what you should use as arguments to your native
// function. They carry extra lifetime information to prevent them escaping
// this context and getting used after being GC'd.
use jni::objects::{JClass, JString};

// This is just a pointer. We'll be returning it from our function. We
// can't return one of the objects with lifetime information because the
// lifetime checker won't let us.
use jni::sys::jstring;

// This keeps Rust from "mangling" the name and making it unique for this
// crate.
#[no_mangle]
pub extern "system" fn Java_iceshelf_clojure_rust_ClojureRust_parseOrgRust(
    env: JNIEnv,
    // This is the class that owns our static method. It's not going to be used,
    // but still must be present to match the expected signature of a static
    // native method.
    _class: JClass,
    keywords: JString,
    unit: JString,
) -> jstring {
    // First, we have to get the string out of Java. Check out the `strings`
    // module for more info on how this works.
    let unit: String = env
        .get_string(unit)
        .expect("Couldn't get java string!")
        .into();

    let keywords: String = env
        .get_string(keywords)
        .expect("Couldn't get keywords string from java")
        .into();

    println!("keywords are {:?}", keywords);


    // let keywords = &args[1];
    let kws: Vec<String> = keywords.split_whitespace().map(|s| s.to_string()).collect();

    let org = Org::parse_custom(
        &unit[..],
        &ParseConfig {
            // custom todo keywords
            todo_keywords: (kws, vec![]),
            ..Default::default()
        },
    );

    // Then we have to create a new Java string to return. Again, more info
    // in the `strings` module.
    // let org = Org::parse(&unit[..]);
    let org_string = to_string(&org).unwrap();
    let output = env
        .new_string(format!("{}", org_string))
        .expect("Couldn't create java string!");

    // Finally, extract the raw pointer to return.
    output.into_inner()
}
