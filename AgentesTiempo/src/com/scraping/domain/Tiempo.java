package com.scraping.domain;

import java.io.Serializable;

public class Tiempo implements Serializable{
	private static final long serialVersionUID = -8589722172762973516L;
	private int hora,  precipitacion, temperatura, v_viento;
	private String clima, d_viento;
	public Tiempo(int hora, int precipitacion, int temperatura, int v_viento, String clima, String d_viento) {
		this.hora = hora;
		this.precipitacion = precipitacion;
		this.temperatura = temperatura;
		this.v_viento = v_viento;
		this.clima = clima;
		this.d_viento = d_viento;
	}
	///////////////////////
	////Getters-Setters////
	///////////////////////
	public int getHora() {
		return hora;
	}
	public void setHora(int hora) {
		this.hora = hora;
	}
	public int getPrecipitacion() {
		return precipitacion;
	}
	public void setPrecipitacion(int precipitacion) {
		this.precipitacion = precipitacion;
	}
	public int getTemperatura() {
		return temperatura;
	}
	public void setTemperatura(int temperatura) {
		this.temperatura = temperatura;
	}
	public int getV_viento() {
		return v_viento;
	}
	public void setV_viento(int v_viento) {
		this.v_viento = v_viento;
	}
	public String getClima() {
		return clima;
	}
	public void setClima(String clima) {
		this.clima = clima;
	}
	public String getD_viento() {
		return d_viento;
	}
	public void setD_viento(String d_viento) {
		this.d_viento = d_viento;
	}
	@Override
	public String toString() {
		return "Tiempo[\n\thora=" + hora + " h,\n\tprecipitacion=" + precipitacion + " l/m^2,\n\ttemperatura=" + temperatura
				+ " ºC,\n\tv_viento=" + v_viento + " km/h, \n\tclima=" + clima + ",\n\td_viento=" + d_viento + "\n]";
	}
	@Override
	public boolean equals(Object objeto) {
		Tiempo tiempo = (Tiempo)objeto;
		return this.hora==tiempo.getHora() && this.v_viento == tiempo.getV_viento();
	}
}
