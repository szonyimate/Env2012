package com.example.env2012;

public class Teeprom {

    TptParms ptary[] = new TptParms[4];
    float rnorm, proffs, prslope, shtrhoffs, shtrhslope, shttmpoffs, shttmpslope;
    char equipid[] = new char[8];
    long humcaldat, ptcaldat, prescaldat, crc0;

    public TptParms[] getPtary() {
        return ptary;
    }

    public void setPtary(TptParms[] ptary) {
        this.ptary = ptary;
    }

    public float getRnorm() {
        return rnorm;
    }

    public void setRnorm(float rnorm) {
        this.rnorm = rnorm;
    }

    public float getProffs() {
        return proffs;
    }

    public void setProffs(float proffs) {
        this.proffs = proffs;
    }

    public float getPrslope() {
        return prslope;
    }

    public void setPrslope(float prslope) {
        this.prslope = prslope;
    }

    public float getShtrhoffs() {
        return shtrhoffs;
    }

    public void setShtrhoffs(float shtrhoffs) {
        this.shtrhoffs = shtrhoffs;
    }

    public float getShtrhslope() {
        return shtrhslope;
    }

    public void setShtrhslope(float shtrhslope) {
        this.shtrhslope = shtrhslope;
    }

    public float getShttmpoffs() {
        return shttmpoffs;
    }

    public void setShttmpoffs(float shttmpoffs) {
        this.shttmpoffs = shttmpoffs;
    }

    public float getShttmpslope() {
        return shttmpslope;
    }

    public void setShttmpslope(float shttmpslope) {
        this.shttmpslope = shttmpslope;
    }

    public char[] getEquipid() {
        return equipid;
    }

    public void setEquipid(char[] equipid) {
        this.equipid = equipid;
    }

    public long getHumcaldat() {
        return humcaldat;
    }

    public void setHumcaldat(long humcaldat) {
        this.humcaldat = humcaldat;
    }

    public long getPtcaldat() {
        return ptcaldat;
    }

    public void setPtcaldat(long ptcaldat) {
        this.ptcaldat = ptcaldat;
    }

    public long getPrescaldat() {
        return prescaldat;
    }

    public void setPrescaldat(long prescaldat) {
        this.prescaldat = prescaldat;
    }

    public long getCrc0() {
        return crc0;
    }

    public void setCrc0(long crc0) {
        this.crc0 = crc0;
    }
}
