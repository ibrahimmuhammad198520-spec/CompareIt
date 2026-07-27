def pad(value, width):
    value = str(value)
    if len(value) > width:
        raise ValueError(f"'{value}' too long for width {width}")
    return value.ljust(width)

def bh(seq4, rest):
    # "BH" + 4-char sequence number (ignored) + rest of header content
    return "BH" + pad(seq4, 4) + rest

def txv(prefix_junk, rrn12, tail):
    """
    Builds a Transaction Detail line where the RRN sits at absolute
    position 122-133 (1-based), i.e. characters 121-132 (0-based).
    prefix_junk fills positions 4..121 (before the RRN).
    """
    body_before_rrn_len = 121 - 3  # positions 4..121 inclusive = 118 chars
    prefix_junk = pad(prefix_junk, body_before_rrn_len)
    line = "TXV" + prefix_junk + pad(rrn12, 12) + tail
    assert line[121:133] == pad(rrn12, 12), f"RRN misaligned: {line[121:133]!r}"
    return line

# ---------------- PRODUCTION FILE ----------------
prod_lines = [
    "FH" + pad("0000000020026060100000020026060102000620260719VER53", 60),
    bh("0001", "FIXIT FACILITIE_DOHA_00000063400000007349"),
    txv("A1", "667848280001", "_AMT0000010050_20260101_AUTH01_STATUS-OK"),
    txv("A2", "667848280002", "_AMT0000020075_20260101_AUTH02_STATUS-OK"),
    txv("A3", "667848280003", "_AMT0000030000_20260102_AUTH03_STATUS-OK"),  # will be MISSING in test
    txv("A4", "667848280004", "_AMT0000040000_20260102_AUTH04_STATUS-OK"),  # AuthCode-ish tail will differ in test
    "BT" + pad("00010000001", 30),
    "FT" + pad("558511908000000179445486300", 40),
]

# ---------------- TEST FILE ----------------
# Differences vs production:
#  - Batch header sequence number changed (positions 3-6) -> must be IGNORED
#  - Transaction records REORDERED (different rows) -> must NOT be a mismatch
#  - RRN 667848280003 REMOVED               -> MISSING record
#  - RRN 667848280005 ADDED                 -> EXTRA record
#  - RRN 667848280004 tail content changed  -> CONTENT DIFFERENCE
test_lines = [
    "FH" + pad("0000000020026060100000020026060102000620260719VER53", 60),
    bh("9999", "FIXIT FACILITIE_DOHA_00000063400000007349"),   # seq differs - ignored
    txv("A2", "667848280002", "_AMT0000020075_20260101_AUTH02_STATUS-OK"),  # reordered: was 2nd, now 1st TXV row
    txv("A1", "667848280001", "_AMT0000010050_20260101_AUTH01_STATUS-OK"),  # reordered: was 1st, now 2nd TXV row
    txv("A4", "667848280004", "_AMT0000040000_20260102_AUTH99_STATUS-FAIL"), # content differs (AUTH99/STATUS-FAIL)
    txv("A5", "667848280005", "_AMT0000050000_20260103_AUTH05_STATUS-OK"),  # extra record
    "BT" + pad("00010000001", 30),
    "FT" + pad("558511908000000179445486300", 40),
]

with open("prod.txt", "w") as fh:
    fh.write("\n".join(prod_lines) + "\n")

with open("test.txt", "w") as fh:
    fh.write("\n".join(test_lines) + "\n")

print("Generated prod.txt and test.txt")
