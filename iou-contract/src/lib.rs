pub mod ledger;
pub mod memory;

use memory::{read_str, fetch_str, get_field};
use ledger::{canton_create, canton_fetch, canton_archive};

// ─────────────────────────────────────────────────────────────────────────────
// Note this is a very low-level API, where Java and Rust talks
// in terms of a pair of (pointer, num bytes) to exchange messages;
// A much more high-level, nicer DSL, which abstracts over the
// serialization-deserialization is possible 
// ─────────────────────────────────────────────────────────────────────────────




// ─────────────────────────────────────────────────────────────────────────────
//  template Iou
//    with
//      issuer : Party
//      owner  : Party
//      amount : Decimal
// ─────────────────────────────────────────────────────────────────────────────

struct Iou<'a> {
    issuer: &'a str,
    owner:  &'a str,
    amount: i64,
}

impl<'a> Iou<'a> {

    // ensure amount > 0.0
    fn ensure(&self) {
        assert!(self.amount > 0, "amount must be positive");
    }

    // signatory issuer
    fn signatories(&self) -> &str {
        self.issuer
    }

    fn to_payload(&self) -> String {
        format!(
            r#"{{"issuer":"{}","owner":"{}","amount":"{}"}}"#,
            self.issuer, self.owner, self.amount
        )
    }

    fn create(&self) -> i64 {
        let tmpl    = "Iou";
        let payload = self.to_payload();
        let sigs    = self.signatories();
        unsafe {
            canton_create(
                tmpl.as_ptr()    as i32, tmpl.len()    as i32,
                payload.as_ptr() as i32, payload.len() as i32,
                sigs.as_ptr()    as i32, sigs.len()    as i32,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  create Iou with issuer owner amount
//  example message: {"issuer":"Alice","owner":"Bob","amount":"100"}
// ─────────────────────────────────────────────────────────────────────────────
#[no_mangle]
pub extern "C" fn iou_create(msg_ptr: i32, msg_len: i32) -> i64 {
    let msg = read_str(msg_ptr, msg_len);

    let iou = Iou {
        issuer: get_field(msg, "issuer"),
        owner:  get_field(msg, "owner"),
        amount: get_field(msg, "amount").parse().unwrap(),
    };

    iou.ensure();
    iou.create()
}

// ─────────────────────────────────────────────────────────────────────────────
//  choice Transfer : ContractId Iou
//    with newOwner : Party
//    controller owner
//    do create this with owner = newOwner
//
//   example message: {"cid":"3f2a1b8c-...","new_owner":"Charlie","actor":"Bob"}
// ─────────────────────────────────────────────────────────────────────────────
#[no_mangle]
pub extern "C" fn iou_transfer(msg_ptr: i32, msg_len: i32) -> i64 {
    let msg       = read_str(msg_ptr, msg_len);
    let cid       = get_field(msg, "cid");
    let new_owner = get_field(msg, "new_owner");
    let actor     = get_field(msg, "actor");

    let payload = fetch_str(unsafe {
        canton_fetch(cid.as_ptr() as i32, cid.len() as i32)
    });

    // controller owner
    let owner = get_field(&payload, "owner");
    assert_eq!(owner, actor, "only the owner can transfer");

    unsafe {
        canton_archive(
            cid.as_ptr()   as i32, cid.len()   as i32,
            actor.as_ptr() as i32, actor.len()  as i32,
        )
    }

    Iou {
        issuer: get_field(&payload, "issuer"),
        owner:  new_owner,
        amount: get_field(&payload, "amount").parse().unwrap(),
    }.create()
}

// ─────────────────────────────────────────────────────────────────────────────
//  choice Redeem : ()
//    controller owner
//    do return ()
//
//   example message: {"cid":"3f2a1b8c-...","actor":"Charlie"}
// ─────────────────────────────────────────────────────────────────────────────
#[no_mangle]
pub extern "C" fn iou_redeem(msg_ptr: i32, msg_len: i32) {
    let msg   = read_str(msg_ptr, msg_len);
    let cid   = get_field(msg, "cid");
    let actor = get_field(msg, "actor");

    let payload = fetch_str(unsafe {
        canton_fetch(cid.as_ptr() as i32, cid.len() as i32)
    });

    // controller owner
    let owner = get_field(&payload, "owner");
    assert_eq!(owner, actor, "only the owner can redeem"); // note runtime check; could be made static

    unsafe {
        canton_archive(
            cid.as_ptr()   as i32, cid.len()   as i32,
            actor.as_ptr() as i32, actor.len()  as i32,
        )
    }
}