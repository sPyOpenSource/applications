/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package test.nfs;

import jx.net.IPAddress;
import jx.rpc.Auth;
import jx.rpc.RPC;
import jx.rpcsvc.nfs2.AttrStat;
import jx.rpcsvc.nfs2.DirOpRes;
import jx.rpcsvc.nfs2.DirPath;
import jx.rpcsvc.nfs2.FHandle;
import jx.rpcsvc.nfs2.NFSCookie;
import jx.rpcsvc.nfs2.NFSData;
import jx.rpcsvc.nfs2.NFSProc;
import jx.rpcsvc.nfs2.Name;
import jx.rpcsvc.nfs2.ReadDirRes;
import jx.rpcsvc.nfs2.ReadLinkRes;
import jx.rpcsvc.nfs2.ReadRes;
import jx.rpcsvc.nfs2.SAttr;
import jx.rpcsvc.nfs2.Stat;
import jx.rpcsvc.nfs2.StatFSRes;

/**
 *
 * @author xuyi
 */
public class NFSProc_Stub implements NFSProc {

    public NFSProc_Stub(RPC rpc, IPAddress rpcHost) {
    }

    void setAuth(Auth a, Auth ct) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public void nullproc() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public AttrStat getattr(FHandle a) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public AttrStat setattr(FHandle file, SAttr attributes) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public void root() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public DirOpRes lookup(FHandle dir, Name name) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public ReadLinkRes readlink(FHandle a) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public ReadRes read(FHandle file, int offset, int count, int totalcount) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public void writeCache() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public AttrStat write(FHandle file, int beginoffset, int offset, int totalcount, NFSData data) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public DirOpRes create(FHandle dir, Name name, SAttr attributes) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public Stat remove(FHandle dir, Name name) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public Stat rename(FHandle fromDir, Name fromName, FHandle toDir, Name toName) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public Stat link(FHandle from, FHandle toDir, Name toName) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public Stat symlink(FHandle fromDir, Name fromName, DirPath to, SAttr attributes) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public DirOpRes mkdir(FHandle dir, Name name, SAttr attributes) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public Stat rmdir(FHandle dir, Name name) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public ReadDirRes readdir(FHandle dir, NFSCookie cookie, int count) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public StatFSRes statfs(FHandle dir) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }
    
}
