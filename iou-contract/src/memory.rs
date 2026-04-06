// ─────────────────────────────────────────────────────────────────────────────
// Memory helpers for serialization/deserialization
// These exist purely because WASM can only pass i32/i64 across the boundary.
// 
// String are always represented by a pair of (pointer, num bytes)
// ─────────────────────────────────────────────────────────────────────────────

// Read a string from WASM memory given a pointer and length
pub fn read_str(ptr: i32, len: i32) -> &'static str {
    unsafe {
        let bytes = core::slice::from_raw_parts(
            ptr as *const u8,
            len as usize
        );
        core::str::from_utf8_unchecked(bytes)
    }
}

// Unpack an i64 (ptr << 32 | len) that Java wrote into WASM memory
// and return the string Java placed there
pub fn fetch_str(packed: i64) -> String {
    let ptr = (packed >> 32) as usize;
    let len = (packed & 0xFFFFFFFF) as usize;
    unsafe {
        let bytes = core::slice::from_raw_parts(ptr as *const u8, len);
        String::from_utf8_unchecked(bytes.to_vec())
    }
}

// Pull a field value out of a simple JSON string
// e.g. get_field({"owner":"Bob"}, "owner") -> "Bob"
pub fn get_field<'a>(json: &'a str, key: &str) -> &'a str {
    let search = format!("\"{}\":\"", key);
    let start  = json.find(search.as_str()).unwrap() + search.len();
    let end    = start + json[start..].find('"').unwrap();
    &json[start..end]
}

#[no_mangle]
pub extern "C" fn alloc(size: usize) -> *mut u8 {
    let mut buf = Vec::with_capacity(size);
    let ptr = buf.as_mut_ptr();
    std::mem::forget(buf);
    ptr
}

#[no_mangle]
pub extern "C" fn dealloc(ptr: *mut u8, size: usize) {
    unsafe {
        drop(Vec::from_raw_parts(ptr, size, size));
    }
}
