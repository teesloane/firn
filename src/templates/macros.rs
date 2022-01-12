// This code is shamelessly borrowed from zola.
// https://github.com/getzola/zola/blob/master/components/templates/src/global_fns/macros.rs

#[macro_export]
macro_rules! required_arg {
    ($ty: ty, $e: expr, $err: expr) => {
        match $e {
            Some(v) => match from_value::<$ty>(v.clone()) {
                Ok(u) => u,
                Err(_) => return Err($err.into()),
            },
            None => return Err($err.into()),
        }
    };
}

#[macro_export]
macro_rules! optional_arg {
    ($ty: ty, $e: expr, $err: expr) => {
        match $e {
            Some(v) => match from_value::<$ty>(v.clone()) {
                Ok(u) => Some(u),
                Err(_) => return Err($err.into()),
            },
            None => None,
        }
    };
}
