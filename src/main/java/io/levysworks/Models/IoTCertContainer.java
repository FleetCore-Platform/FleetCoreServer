package io.levysworks.Models;

public class IoTCertContainer {
    private final String certificatePEM;
    private final String certificateARN;
    public IoTCertContainer(String certificatePEM, String certificateARN) {
        this.certificatePEM = certificatePEM;
        this.certificateARN = certificateARN;
    }

    public String getCertificatePEM() {
        return certificatePEM;
    }

    public String getCertificateARN() {
        return certificateARN;
    }
}
