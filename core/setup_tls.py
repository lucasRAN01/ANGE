"""Create a local CA and LAN server certificate. Never publish .local/ keys."""
import argparse
from datetime import datetime, timedelta, timezone
import ipaddress
from pathlib import Path
from cryptography import x509
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import rsa
from cryptography.x509.oid import NameOID, ExtendedKeyUsageOID

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--name", required=True, help="DNS name used by the phone, e.g. mon-pc.local")
    parser.add_argument("--ip", required=True, help="Private LAN IP of the PC")
    args = parser.parse_args()
    address = ipaddress.ip_address(args.ip)
    if not address.is_private:
        parser.error("Use a private LAN IP, not a public address.")
    out = Path(__file__).resolve().parent / ".local"
    out.mkdir(exist_ok=True)
    for name in ("ca-key.pem", "server-key.pem", "server-cert.pem", "ange-ca.crt"):
        if (out / name).exists():
            parser.error("TLS files already exist. Preserve them or archive them manually before regenerating.")
    now = datetime.now(timezone.utc)
    ca_key = rsa.generate_private_key(public_exponent=65537, key_size=3072)
    server_key = rsa.generate_private_key(public_exponent=65537, key_size=2048)
    ca_name = x509.Name([x509.NameAttribute(NameOID.COMMON_NAME, "ANGE Local Personal CA")])
    ca = (x509.CertificateBuilder().subject_name(ca_name).issuer_name(ca_name).public_key(ca_key.public_key())
        .serial_number(x509.random_serial_number()).not_valid_before(now-timedelta(minutes=5)).not_valid_after(now+timedelta(days=365))
        .add_extension(x509.BasicConstraints(ca=True, path_length=0), critical=True)
        .add_extension(x509.KeyUsage(digital_signature=True, content_commitment=False, key_encipherment=False,
            data_encipherment=False, key_agreement=False, key_cert_sign=True, crl_sign=True,
            encipher_only=False, decipher_only=False), critical=True).sign(ca_key, hashes.SHA256()))
    cert = (x509.CertificateBuilder().subject_name(x509.Name([x509.NameAttribute(NameOID.COMMON_NAME, args.name)]))
        .issuer_name(ca_name).public_key(server_key.public_key()).serial_number(x509.random_serial_number())
        .not_valid_before(now-timedelta(minutes=5)).not_valid_after(now+timedelta(days=90))
        .add_extension(x509.BasicConstraints(ca=False, path_length=None), critical=True)
        .add_extension(x509.SubjectAlternativeName([x509.DNSName(args.name), x509.IPAddress(address)]), critical=False)
        .add_extension(x509.ExtendedKeyUsage([ExtendedKeyUsageOID.SERVER_AUTH]), critical=False)
        .sign(ca_key, hashes.SHA256()))
    for name, key in (("ca-key.pem", ca_key), ("server-key.pem", server_key)):
        (out/name).write_bytes(key.private_bytes(serialization.Encoding.PEM, serialization.PrivateFormat.PKCS8, serialization.NoEncryption()))
    (out/"ange-ca.crt").write_bytes(ca.public_bytes(serialization.Encoding.PEM))
    (out/"server-cert.pem").write_bytes(cert.public_bytes(serialization.Encoding.PEM))
    print("Certificats créés dans core/.local. Installer seulement ange-ca.crt sur le téléphone.")
    print("Garder les clés privées sur le PC. Certificat serveur valable 90 jours.")

if __name__ == "__main__":
    main()
