# Firejail profile for NPM

# Persistent global definitions

include /etc/firejail/disable-common.inc
include /etc/firejail/disable-passwdmgr.inc
include /etc/firejail/disable-programs.inc

caps.drop all
ipc-namespace
netfilter
no3d
nodvd
nogroups
nonewprivs
noroot
nosound
notv
novideo
protocol unix,inet,inet6,netlink,packet
seccomp
shell none
disable-mnt
private-dev
private-tmp
rlimit-as 2147483648
rlimit-cpu 600
timeout 00:10:00

whitelist ~/.npm
whitelist ~/.config/configstore
whitelist ~/.npmrc
private-etc npmrc,ssl,hosts,nsswitch.conf,resolv.conf
blacklist /var
#private-bin node,npm,env
# private-lib
blacklist /usr/share
