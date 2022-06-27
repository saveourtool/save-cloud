#!/usr/bin/env sh

# Script from https://gvisor.dev/docs/user_guide/install/#install-latest

cat > gvisor-install.sh << 'EOF'
ARCH=$(uname -m)
URL=https://storage.googleapis.com/gvisor/releases/release/latest/${ARCH}
wget -nv ${URL}/runsc ${URL}/runsc.sha512 ${URL}/containerd-shim-runsc-v1 ${URL}/containerd-shim-runsc-v1.sha512
sha512sum -c runsc.sha512 -c containerd-shim-runsc-v1.sha512
rm -f *.sha512
chmod a+rx runsc containerd-shim-runsc-v1
mv runsc containerd-shim-runsc-v1 /usr/local/bin
EOF
cp gvisor-install.sh /k8s-node

ls -l /k8s-node
# Run the following steps on the host system
/usr/bin/nsenter -m/proc/1/ns/mnt -- ls -l /tmp/gvisor
/usr/bin/nsenter -m/proc/1/ns/mnt -- chmod u+x /tmp/gvisor/gvisor-install.sh
/usr/bin/nsenter -m/proc/1/ns/mnt /tmp/gvisor/gvisor-install.sh
/usr/bin/nsenter -m/proc/1/ns/mnt -- /usr/local/bin/runsc install
/usr/bin/nsenter -m/proc/1/ns/mnt -- systemctl reload docker

sleep infinity