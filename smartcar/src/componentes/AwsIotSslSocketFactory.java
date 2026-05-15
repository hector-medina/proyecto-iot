package componentes;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

public final class AwsIotSslSocketFactory {

    private static final String TLS_VERSION = "TLSv1.2";
    private static final char[] KEYSTORE_PASSWORD = "aws-iot".toCharArray();

    private AwsIotSslSocketFactory() {
    }

    public static SSLSocketFactory create(String rootCaPath, String certificatePath, String privateKeyPath) throws Exception {
        return create(Path.of(rootCaPath), Path.of(certificatePath), Path.of(privateKeyPath));
    }

    public static SSLSocketFactory create(Path rootCaPath, Path certificatePath, Path privateKeyPath) throws Exception {
        Certificate rootCa = loadCertificate(rootCaPath);
        Certificate certificate = loadCertificate(certificatePath);
        PrivateKey privateKey = loadPrivateKey(privateKeyPath);

        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        trustStore.load(null, null);
        trustStore.setCertificateEntry("amazon-root-ca", rootCa);

        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(trustStore);

        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null, null);
        keyStore.setKeyEntry("device-certificate", privateKey, KEYSTORE_PASSWORD, new Certificate[] { certificate });

        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, KEYSTORE_PASSWORD);

        SSLContext context = SSLContext.getInstance(TLS_VERSION);
        context.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), new SecureRandom());
        return context.getSocketFactory();
    }

    private static Certificate loadCertificate(Path certificatePath) throws Exception {
        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        try (ByteArrayInputStream input = new ByteArrayInputStream(Files.readAllBytes(certificatePath))) {
            return certificateFactory.generateCertificate(input);
        }
    }

    private static PrivateKey loadPrivateKey(Path privateKeyPath) throws Exception {
        String pem = Files.readString(privateKeyPath, StandardCharsets.US_ASCII);
        byte[] keyBytes;

        if (pem.contains("BEGIN RSA PRIVATE KEY")) {
            keyBytes = wrapPkcs1RsaPrivateKey(readPemBlock(pem, "RSA PRIVATE KEY"));
        } else if (pem.contains("BEGIN PRIVATE KEY")) {
            keyBytes = readPemBlock(pem, "PRIVATE KEY");
        } else {
            throw new IllegalArgumentException("Unsupported private key format: " + privateKeyPath);
        }

        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        return KeyFactory.getInstance("RSA").generatePrivate(keySpec);
    }

    private static byte[] readPemBlock(String pem, String type) {
        String begin = "-----BEGIN " + type + "-----";
        String end = "-----END " + type + "-----";
        int beginIndex = pem.indexOf(begin);
        int endIndex = pem.indexOf(end);

        if (beginIndex < 0 || endIndex < 0 || endIndex <= beginIndex) {
            throw new IllegalArgumentException("PEM block not found: " + type);
        }

        String base64 = pem.substring(beginIndex + begin.length(), endIndex).replaceAll("\\s", "");
        return Base64.getDecoder().decode(base64);
    }

    private static byte[] wrapPkcs1RsaPrivateKey(byte[] pkcs1Key) {
        byte[] rsaEncryptionOid = new byte[] {
                0x30, 0x0d,
                0x06, 0x09,
                0x2a, (byte) 0x86, 0x48, (byte) 0x86, (byte) 0xf7, 0x0d, 0x01, 0x01, 0x01,
                0x05, 0x00
        };
        byte[] version = new byte[] { 0x02, 0x01, 0x00 };
        byte[] privateKey = der(0x04, pkcs1Key);
        byte[] privateKeyInfo = concat(version, rsaEncryptionOid, privateKey);
        return der(0x30, privateKeyInfo);
    }

    private static byte[] der(int tag, byte[] value) {
        return concat(new byte[] { (byte) tag }, derLength(value.length), value);
    }

    private static byte[] derLength(int length) {
        if (length < 128) {
            return new byte[] { (byte) length };
        }

        int temp = length;
        int bytesRequired = 0;
        while (temp > 0) {
            temp >>= 8;
            bytesRequired++;
        }

        byte[] encoded = new byte[bytesRequired + 1];
        encoded[0] = (byte) (0x80 | bytesRequired);
        for (int i = bytesRequired; i > 0; i--) {
            encoded[i] = (byte) (length & 0xff);
            length >>= 8;
        }
        return encoded;
    }

    private static byte[] concat(byte[]... arrays) {
        int length = 0;
        for (byte[] array : arrays) {
            length += array.length;
        }

        byte[] result = new byte[length];
        int offset = 0;
        for (byte[] array : arrays) {
            System.arraycopy(array, 0, result, offset, array.length);
            offset += array.length;
        }
        return result;
    }
}
