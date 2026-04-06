//This API will be provided by the Java side

extern "C" {
    pub fn canton_create(
        tmpl_ptr: i32, tmpl_len: i32,
        payload_ptr: i32, payload_len: i32,
        sigs_ptr: i32, sigs_len: i32,
    ) -> i64;

    pub fn canton_fetch(cid_ptr: i32, cid_len: i32) -> i64;

    pub fn canton_archive(
        cid_ptr: i32, cid_len: i32,
        actor_ptr: i32, actor_len: i32,
    );
}
