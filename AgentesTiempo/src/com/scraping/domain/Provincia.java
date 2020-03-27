package com.scraping.domain;

import java.io.Serializable;

public class Provincia implements Serializable {
	private String href;
	private String nombre;
	public Provincia(String href, String nombre) {
		this.href = href;
		this.nombre = nombre;
	}
	public Provincia(String nombre) {
		this.nombre = nombre;
		this.href = "";
	}
	///////////////
	////Get-Set////
	///////////////
	public String getHref() {
		return href;
	}
	public String getNombre() {
		return nombre;
	}
	public void setHref(String href) {
		this.href = href;
	}
	public void setNombre(String nombre) {
		this.nombre = nombre;
	}
	////////////////////////
	////Metodos publicos////
	////////////////////////
	@Override
	public String toString() {
		return nombre;
	}
	@Override
	public boolean equals(Object provincia) {
		Provincia p = (Provincia) provincia;
		return this.nombre.equalsIgnoreCase(p.getNombre());
	}
}
