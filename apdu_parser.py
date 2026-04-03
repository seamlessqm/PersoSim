#!/usr/bin/env python3
import sys

DG_NAMES = {
    0x01: "DG1 - Document Type",
    0x02: "DG2 - Issuing State",
    0x03: "DG3 - Date of Expiry",
    0x04: "DG4 - Given Names",
    0x05: "DG5 - Family Names",
    0x06: "DG6 - Religious/Artistic Name",
    0x07: "DG7 - Academic Title",
    0x08: "DG8 - Date of Birth",
    0x09: "DG9 - Place of Birth",
    0x0A: "DG10 - Nationality",
    0x0B: "DG11 - Sex",
    0x0C: "DG12 - Optional Data",
    0x0D: "DG13 - Birth Name",
    0x0E: "DG14 - Written Signature",
    0x0F: "DG15 - Date of Issuance",
    0x10: "DG16 - RFU",
    0x11: "DG17 - Place of Residence",
    0x12: "DG18 - Municipality ID",
    0x13: "DG19 - Residence Permit I",
    0x14: "DG20 - Residence Permit II",
    0x15: "DG21 - Phone Number",
    0x16: "DG22 - Email Address",
}

INS_NAMES = {
    0xA4: "SELECT",
    0xB0: "READ BINARY",
    0xB1: "READ BINARY ODD",
    0xD6: "UPDATE BINARY",
    0xD7: "UPDATE BINARY ODD",
    0x20: "VERIFY",
    0x22: "MANAGE SECURITY ENVIRONMENT",
    0x24: "CHANGE REFERENCE DATA",
    0x2C: "RESET RETRY COUNTER",
    0x86: "GENERAL AUTHENTICATE",
    0x0E: "ERASE BINARY",
}


def parse_apdu(hex_string):
    hex_string = hex_string.replace(" ", "")
    if len(hex_string) < 8:
        print("APDU too short")
        return

    cla = int(hex_string[0:2], 16)
    ins = int(hex_string[2:4], 16)
    p1 = int(hex_string[4:6], 16)
    p2 = int(hex_string[6:8], 16)
    data = hex_string[8:] if len(hex_string) > 8 else ""

    ins_name = INS_NAMES.get(ins, f"UNKNOWN (0x{ins:02X})")
    print(f"CLA: 0x{cla:02X}")
    print(f"INS: 0x{ins:02X} → {ins_name}")
    print(f"P1:  0x{p1:02X}")
    print(f"P2:  0x{p2:02X}")

    if ins in (0xB0, 0xD6, 0xB1, 0xD7):
        if p1 & 0x80:
            sfi = p1 & 0x1F
            dg_name = DG_NAMES.get(sfi, f"Unknown DG (0x{sfi:02X})")
            offset = p2
            print(f"Short File Identifier: 0x{sfi:02X} → {dg_name}")
            print(f"Offset: {offset}")
        else:
            offset = (p1 << 8) | p2
            print(f"Offset: {offset}")

    if data:
        print(f"Data: {data}")


if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: ./apdu_parser.py <hex_apdu>")
        print("Example: ./apdu_parser.py 00B08D00000000")
        sys.exit(1)
    parse_apdu(sys.argv[1])
