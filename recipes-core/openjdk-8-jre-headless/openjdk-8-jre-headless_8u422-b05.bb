SUMMARY = "Self-built OpenJDK headless JRE for Java 8 from source using Temurin build scripts"
HOMEPAGE = "https://adoptium.net"
LICENSE = "GPL-2.0-with-classpath-exception"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/GPL-2.0-with-classpath-exception;md5=6133e6794362eff6641708cfcc075b80"
COMPATIBLE_HOST = "(x86_64|arm|aarch64).*-linux"

# Dynamic: Set OPENJDK_BUILD_NUMBER and OPENJDK_UPDATE_VERSION from PV
python () {
    import re
    pv = d.getVar('PV')
    # Expected format: "8u422-b05" => update version: "422", build number: "b05"
    m = re.match(r'8u(\d+)-(b\d+)', pv)
    if m:
        d.setVar('OPENJDK_UPDATE_VERSION', m.group(1))
        d.setVar('OPENJDK_BUILD_NUMBER', m.group(2))
}

# Minimal runtime dependencies for a headless system
JVM_RDEPENDS:aarch64 = " freetype (>= 2.11) glibc (>= 2.12) libgcc (>= 4.2) "
JVM_RDEPENDS:arm = " freetype (>= 2.11) glibc (>= 2.12) libatomic (>= 1.0) libgcc (>= 3.5) "
JVM_RDEPENDS:x86-64 = " freetype (>= 2.11) glibc (>= 2.12) libgcc (>= 4.2) "
RDEPENDS:${PN} = "${JVM_RDEPENDS}"

# Boot JDK parameter
BOOTJDK_API_RELEASE_NAME = "jdk${PV}"
BOOTJDK_API_IMAGE_TYPE = "jdk"
BOOTJDK_API_ARCH = "${@ 'x64' if d.getVar('BUILD_ARCH') == 'x86-64' else d.getVar('BUILD_ARCH') }"
BOOTJDK_CHECKSUM:aarch64 = "8fbefff2c578f73d95118d830347589ddc9aa84510200a5a5001901c2dea4810"
BOOTJDK_CHECKSUM:arm = "13bdefdeae6f18bc9c87bba18c853b8b12c5442ce07ff0a3956ce28776d695ff"
BOOTJDK_CHECKSUM:x86-64 = "0ac516cc1eadffb4cd3cfc9736a33d58ea6a396bf85729036c973482f7c063d9"

# Sources:
# We only fetch a boot kit (OpenJDK 8 JDK) and build scripts (v2023.01.03),
# because the OpenJDK sources get fetched internally by the build script.
SRC_URI = "\
  https://api.adoptium.net/v3/binary/version/${BOOTJDK_API_RELEASE_NAME}/linux/${BOOTJDK_API_ARCH}/${BOOTJDK_API_IMAGE_TYPE}/hotspot/normal/eclipse;name=bootjdk;downloadfilename=${BPN}-${BOOTJDK_API_ARCH}-${PV}.tar.gz;subdir=bootjdk;striplevel=1 \
  https://github.com/adoptium/temurin-build/archive/refs/tags/v2023.01.03.tar.gz;name=compilescript;downloadfilename=temurin-build-v2023.01.03.tar.gz;subdir=temurin-build \
"
SRC_URI[bootjdk.sha256sum] = "${BOOTJDK_CHECKSUM}"
SRC_URI[compilescript.sha256sum] = "9239a72b67d765a0dcd298e51f2a4c089d4988519c5079096b504587c9bf5f41"

# Configuration is done by build script
do_configure[noexec] = "1"

# Path to Boot-JDK
BOOT_JDK ?= "${WORKDIR}/bootjdk"

do_compile() {
    cd ${WORKDIR}/temurin-build
    ./makejdk-any-platform.sh --cross-compile --create-jre-image --make-exploded-image \
        -J ${BOOT_JDK} -d ${WORKDIR}/build-output jdk8u -b ${OPENJDK_BUILD_NUMBER} -u ${OPENJDK_UPDATE_VERSION} \
        --skip-freetype --freetype-version 2.11 --freetype-dir ${libdir}
}

libdir_jre = "${libdir}/jvm/${BPN}"

do_install() {
    install -d ${D}${libdir_jre}
    cp -R ${WORKDIR}/build-output/j2re/* ${D}${libdir_jre}/
}

RPROVIDES:${PN} = "java2-runtime"
FILES:${PN} = "${libdir_jre}"

inherit update-alternatives
ALTERNATIVE_PRIORITY = "100"
ALTERNATIVE:${PN} = "java keytool"
ALTERNATIVE_LINK_NAME[java] = "${bindir}/java"
ALTERNATIVE_TARGET[java] = "${libdir_jre}/bin/java"
ALTERNATIVE_LINK_NAME[keytool] = "${bindir}/keytool"
ALTERNATIVE_TARGET[keytool] = "${libdir_jre}/bin/keytool"

