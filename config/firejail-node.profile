# Firejail profile for NODE

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
protocol unix,inet,inet6
seccomp
shell none
disable-mnt
private-dev
private-tmp
rlimit-as 1073741824
rlimit-cpu 600
timeout 00:10:00

private-etc npmrc,ssl,hosts,nsswitch.conf,resolv.conf
blacklist /var
private-bin node
blacklist /usr/share