package com.openiot.cloud.cfg.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openiot.cloud.base.mongo.model.Device;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class PlcManagerConfig {
    private static final String K_NAME = "name";
    private static final String K_IP = "ip";
    private static final String K_PORT = "port";
    private static final String K_kEY = "key";
    private static final String K_HB_MS = "hb_ms";
    private static final String K_LOSS_ACT = "loss_action";
    private static final String K_ROLE = "role";
    private static final String K_PLC_SFT = "plc-runtime";
    private static final String K_PLC_SFT_VER = "plc-runtime-ver";
    private static final String K_PLC_APP = "plc-app";
    private static final String K_PLC_APP_VER = "plc-app-ver";
    private static final String K_START = "start";
    private static final String K_DEBUG = "debug";
    private static final String K_STANDBY_L = "local-standby";
    private static final String K_STANDBY_R = "remote-standby";

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public static class PeerEntity {
        @JsonProperty(K_NAME)
        String name;

        @JsonProperty(K_IP)
        String ip;

        @JsonProperty(K_PORT)
        Integer port;

        @JsonProperty(K_kEY)
        String key;

        @JsonProperty(K_HB_MS)
        Integer hbInMs;

        @JsonProperty(K_LOSS_ACT)
        String lossAction;

        @JsonProperty(K_ROLE)
        String role;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getIp() {
            return ip;
        }

        public void setIp(String ip) {
            this.ip = ip;
        }

        public Integer getPort() {
            return port;
        }

        public void setPort(Integer port) {
            this.port = port;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public Integer getHbInMs() {
            return hbInMs;
        }

        public void setHbInMs(Integer hbInMs) {
            this.hbInMs = hbInMs;
        }

        public String getLossAction() {
            return lossAction;
        }

        public void setLossAction(String lossAction) {
            this.lossAction = lossAction;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        @Override
        public String toString() {
            return "PeerEntity{" +
                    "name='" + name + '\'' +
                    ", ip='" + ip + '\'' +
                    ", port=" + port +
                    ", key='" + key + '\'' +
                    ", hbInMs=" + hbInMs +
                    ", lossAction='" + lossAction + '\'' +
                    ", role='" + role + '\'' +
                    '}';
        }
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public static class VirtualPlc {
        @JsonProperty(K_NAME)
        String name;

        @JsonProperty(K_PLC_SFT)
        String plcRuntimeSoftware;

        @JsonProperty(K_PLC_SFT_VER)
        String plcRuntimeSoftwareVer;

        @JsonProperty(K_PLC_APP)
        String plcApp;

        @JsonProperty(K_PLC_APP_VER)
        String plcAppVer;

        @JsonProperty(K_START)
        String startMode;

        @JsonProperty(K_DEBUG)
        Boolean debugMode;

        @JsonProperty(K_STANDBY_L)
        Boolean localStandby;

        @JsonProperty(K_STANDBY_R)
        Boolean remoteStandby;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getPlcRuntimeSoftware() {
            return plcRuntimeSoftware;
        }

        public void setPlcRuntimeSoftware(String plcRuntimeSoftware) {
            this.plcRuntimeSoftware = plcRuntimeSoftware;
        }

        public String getPlcRuntimeSoftwareVer() {
            return plcRuntimeSoftwareVer;
        }

        public void setPlcRuntimeSoftwareVer(String plcRuntimeSoftwareVer) {
            this.plcRuntimeSoftwareVer = plcRuntimeSoftwareVer;
        }

        public String getPlcApp() {
            return plcApp;
        }

        public void setPlcApp(String plcApp) {
            this.plcApp = plcApp;
        }

        public String getPlcAppVer() {
            return plcAppVer;
        }

        public void setPlcAppVer(String plcAppVer) {
            this.plcAppVer = plcAppVer;
        }

        public String getStartMode() {
            return startMode;
        }

        public void setStartMode(String startMode) {
            this.startMode = startMode;
        }

        public Boolean getDebugMode() {
            return debugMode;
        }

        public void setDebugMode(Boolean debugMode) {
            this.debugMode = debugMode;
        }

        public Boolean getLocalStandby() {
            return localStandby;
        }

        public void setLocalStandby(Boolean localStandby) {
            this.localStandby = localStandby;
        }

        public Boolean getRemoteStandby() {
            return remoteStandby;
        }

        public void setRemoteStandby(Boolean remoteStandby) {
            this.remoteStandby = remoteStandby;
        }

        @Override
        public String toString() {
            return "VirtualPlc{" +
                    "name='" + name + '\'' +
                    ", plcRuntimeSoftware='" + plcRuntimeSoftware + '\'' +
                    ", plcRuntimeSoftwareVer='" + plcRuntimeSoftwareVer + '\'' +
                    ", plcApp='" + plcApp + '\'' +
                    ", plcAppVer='" + plcAppVer + '\'' +
                    ", startMode='" + startMode + '\'' +
                    ", debugMode=" + debugMode +
                    ", localStandby=" + localStandby +
                    ", remoteStandby=" + remoteStandby +
                    '}';
        }
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public static class Address {
        @JsonProperty(K_IP)
        String ip;

        @JsonProperty(K_PORT)
        Integer port;

        public String getIp() {
            return ip;
        }

        public void setIp(String ip) {
            this.ip = ip;
        }

        public Integer getPort() {
            return port;
        }

        public void setPort(Integer port) {
            this.port = port;
        }

        @Override
        public String toString() {
            return "Address{" +
                    "ip='" + ip + '\'' +
                    ", port=" + port +
                    '}';
        }
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public static class RealPlc {
        @JsonProperty(K_NAME)
        String name;

        @JsonProperty(K_PLC_SFT)
        String plcRuntimeSoftware;

        @JsonProperty(K_PLC_SFT_VER)
        String plcRuntimeSoftwareVer;

        @JsonProperty(K_PLC_APP)
        String plcApp;

        @JsonProperty(K_PLC_APP_VER)
        String plcAppVer;

        @JsonProperty("addr")
        Address addr;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getPlcRuntimeSoftware() {
            return plcRuntimeSoftware;
        }

        public void setPlcRuntimeSoftware(String plcRuntimeSoftware) {
            this.plcRuntimeSoftware = plcRuntimeSoftware;
        }

        public String getPlcRuntimeSoftwareVer() {
            return plcRuntimeSoftwareVer;
        }

        public void setPlcRuntimeSoftwareVer(String plcRuntimeSoftwareVer) {
            this.plcRuntimeSoftwareVer = plcRuntimeSoftwareVer;
        }

        public String getPlcApp() {
            return plcApp;
        }

        public void setPlcApp(String plcApp) {
            this.plcApp = plcApp;
        }

        public String getPlcAppVer() {
            return plcAppVer;
        }

        public void setPlcAppVer(String plcAppVer) {
            this.plcAppVer = plcAppVer;
        }

        public Address getAddr() {
            return addr;
        }

        public void setAddr(Address addr) {
            this.addr = addr;
        }

        @Override
        public String toString() {
            return "RealPlc{" +
                    "name='" + name + '\'' +
                    ", plcRuntimeSoftware='" + plcRuntimeSoftware + '\'' +
                    ", plcRuntimeSoftwareVer='" + plcRuntimeSoftwareVer + '\'' +
                    ", plcApp='" + plcApp + '\'' +
                    ", plcAppVer='" + plcAppVer + '\'' +
                    ", addr=" + addr +
                    '}';
        }
    }

    @JsonProperty("id")
    String id;

    @JsonProperty("credential")
    String credential;

    @JsonProperty("peer")
    PeerEntity peer;

    @JsonProperty("virtual-plcs")
    List<VirtualPlc> vPlcs;

    @JsonProperty("real-plcs")
    List<RealPlc> rPlcs;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCredential() {
        return credential;
    }

    public void setCredential(String credential) {
        this.credential = credential;
    }

    public PeerEntity getPeer() {
        return peer;
    }

    public void setPeer(PeerEntity peer) {
        this.peer = peer;
    }

    public List<VirtualPlc> getvPlcs() {
        return vPlcs;
    }

    public void setvPlcs(List<VirtualPlc> vPlcs) {
        this.vPlcs = vPlcs;
    }

    public List<RealPlc> getrPlcs() {
        return rPlcs;
    }

    public void setrPlcs(List<RealPlc> rPlcs) {
        this.rPlcs = rPlcs;
    }

    @Override
    public String toString() {
        return "PlcManagerConfig{" +
                "id='" + id + '\'' +
                ", credential='" + credential + '\'' +
                ", peer=" + peer +
                ", vPlcs=" + vPlcs +
                ", rPlcs=" + rPlcs +
                '}';
    }

    public static PlcManagerConfig from(Optional<Device> gw, Optional<List<Device>> vplcs, Optional<List<Device>> rplcs) {
        return gw.map(d -> {
            PlcManagerConfig plcMgrCfg = new PlcManagerConfig();

            //1. for plcMgrCfg.peer
            plcMgrCfg.setId(d.getId());
            String ip = getDeviceUcOrAs(gw, K_IP);
            String name = getDeviceUcOrAs(gw, K_NAME);
            if( ip!=null && name!=null ){
                plcMgrCfg.peer = new PeerEntity();
                plcMgrCfg.peer.setName(name);
                plcMgrCfg.peer.setIp(ip);
                plcMgrCfg.peer.setKey(getDeviceUcOrAs(gw, K_kEY));
                plcMgrCfg.peer.setLossAction(getDeviceUcOrAs(gw, K_LOSS_ACT));
                plcMgrCfg.peer.setRole(getDeviceUcOrAs(gw, K_ROLE));
                Integer value = Optional.ofNullable(getDeviceUcOrAs(gw, K_PORT))
                        .map(v->Integer.valueOf(v)).orElse(null);
                plcMgrCfg.peer.setPort(value);
                value = Optional.ofNullable(getDeviceUcOrAs(gw, K_HB_MS))
                        .map(v->Integer.valueOf(v)).orElse(null);
                plcMgrCfg.peer.setHbInMs(value);
            }

            //2. for plcMgrCfg.virtual-plc
            List<VirtualPlc> vPlcS = vplcs.filter(vps -> !vps.isEmpty())
                    .map(vps -> {
                        return vps.stream().map(vp -> {
                            VirtualPlc vPlc = new VirtualPlc();
                            Optional<Device> oPv = Optional.ofNullable(vp);
                            vPlc.setName(vp.getName());
                            vPlc.setPlcRuntimeSoftware(getDeviceUc(oPv, K_PLC_SFT));
                            vPlc.setPlcRuntimeSoftwareVer(getDeviceUc(oPv, K_PLC_SFT_VER));
                            vPlc.setPlcApp(getDeviceUc(oPv, K_PLC_APP));
                            vPlc.setPlcAppVer(getDeviceUc(oPv, K_PLC_APP_VER));
                            vPlc.setStartMode(getDeviceUc(oPv, K_START));
                            Boolean cv = Optional.ofNullable(getDeviceUc(oPv, K_DEBUG))
                                    .map(v -> Boolean.valueOf(v)).orElse(null);
                            vPlc.setDebugMode(cv);
                            cv = Optional.ofNullable(getDeviceUc(oPv, K_STANDBY_L))
                                    .map(v -> Boolean.valueOf(v)).orElse(null);
                            vPlc.setLocalStandby(cv);
                            cv = Optional.ofNullable(getDeviceUc(oPv, K_STANDBY_R))
                                    .map(v -> Boolean.valueOf(v)).orElse(null);
                            vPlc.setRemoteStandby(cv);
                            return vPlc;
                        }).collect(Collectors.toList());
                    })
                    .orElse(null);
            if(vPlcS!=null) plcMgrCfg.setvPlcs(vPlcS);

            //3. for plcMgrCfg.real-plc
            List<RealPlc> rPlcS = rplcs.filter(vps -> !vps.isEmpty())
                    .map(rps -> {
                        return rps.stream().map(rp -> {
                            RealPlc rPlc = new RealPlc();
                            Optional<Device> oPv = Optional.ofNullable(rp);
                            rPlc.setName(rp.getName());
                            rPlc.setPlcRuntimeSoftware(getDeviceUc(oPv, K_PLC_SFT));
                            rPlc.setPlcRuntimeSoftwareVer(getDeviceUc(oPv, K_PLC_SFT_VER));
                            rPlc.setPlcApp(getDeviceUc(oPv, K_PLC_APP));
                            rPlc.setPlcAppVer(getDeviceUc(oPv, K_PLC_APP_VER));

                            Address addr = new Address();
                            Integer port = Optional.ofNullable(getDeviceUc(oPv, K_PORT))
                                    .map(v->Integer.valueOf(v)).orElse(null);
                            addr.setIp(getDeviceUc(oPv, K_IP));
                            addr.setPort(port);
                            rPlc.setAddr(addr);
                            return rPlc;
                        }).collect(Collectors.toList());
                    })
                    .orElse(null);
            if(rPlcS!=null) plcMgrCfg.setrPlcs(rPlcS);

            return plcMgrCfg;
        }).orElse(null);
    }

    public static String getDeviceUc(Optional<Device> gw, String kName) {
        return gw.map(d->{
            return Optional.ofNullable(d.getConfig()).map(c->{
                return Optional.ofNullable(c.getUserCfgs()).map(css->{
                    return css.stream().filter(cs->cs.getCn().equals(kName))
                            .findFirst().map(i->i.getCv()).orElse(null);
                        }
                ).orElse(null);
                    }
            ).orElse(null);
        }).orElse(null);
    }

    public static String getDeviceAs(Optional<Device> gw, String kName) {
        return gw.map(d->{
            return Optional.ofNullable(d.getConfig()).map(c->{
                return Optional.ofNullable(c.getAttributes()).map(css->{
                    return css.stream().filter(cs->cs.getAn().equals(kName))
                            .findFirst().map(i->i.getAv()).orElse(null);
                        }
                ).orElse(null);
                    }
            ).orElse(null);
        }).orElse(null);
    }

    public static String getDeviceUcOrAs(Optional<Device> gw, String kName) {
        return Optional.ofNullable(getDeviceUc(gw, kName)).orElse(getDeviceAs(gw, kName));
    }

    public String toJsonString() {
        try {
            return new ObjectMapper().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }
    }
}
