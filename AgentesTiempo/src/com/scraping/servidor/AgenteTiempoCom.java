package com.scraping.servidor;

import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.FIPAAgentManagement.NotUnderstoodException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import jade.proto.SubscriptionResponder;
import jade.proto.SubscriptionResponder.Subscription;
import jade.proto.SubscriptionResponder.SubscriptionManager;

import com.scraping.domain.Provincia;
import com.scraping.domain.Tiempo;
import com.scraping.persistencia.Scraper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class AgenteTiempoCom extends Agent {
	//Se crea una tabla indexada donde guardaremos las suscripciones realizadas.
    private Set<Subscription> suscripciones = new HashSet<Subscription>();
    private List<Provincia> l_provincias;
    private Scraper scraping;
 
    protected void setup() {
        System.out.println(this.getLocalName() + ": Esperando suscripciones...");
        
        scraping = new Scraper("https://www.tiempo.com/");
        cargarProvincias();
        MessageTemplate template = SubscriptionResponder.createMessageTemplate(ACLMessage.SUBSCRIBE);
 
        this.addBehaviour(new EnviarSemanal(this, (long) 10000));
        SubscriptionManager gestor = new SubscriptionManager() {
 
            public boolean register(Subscription suscripcion) {
                suscripciones.add(suscripcion);
                return true;
            }
            public boolean deregister(Subscription suscripcion) {
                suscripciones.remove(suscripcion);
                return true;
            }
        };
        this.addBehaviour(new HacerSuscripcion(this, template, gestor));
    }
    private boolean compruebaMensaje(Object p_solicitada) {
    	boolean isContain = false;
    	try {
    		//System.out.println(AgenteMeteosat.this.getLocalName()+":\n\tComprobando ciudad: "+p_solicitada.toString());
    		isContain = l_provincias.contains(p_solicitada);
    	}catch(Exception e) {
    		System.out.println(AgenteTiempoCom.this.getLocalName()+":\n\tError-->"+e.getMessage());
    	}
    	return isContain;
    }
    private void cargarProvincias() {
    	l_provincias = new ArrayList<>();
    	// Compruebo si el protocolo http es 200
        if (scraping.getStatusConnectionCode() == 200) {
			Document document = scraping.getHtmlDocument();// Obtengo el HTML de la web en un objeto Document
			Elements provincias = document.select("a.enlace");
    		for(Element e : provincias) {
    			l_provincias.add(new Provincia(e.attr("href"),e.text()));
    		}
        }else
            System.out.println("El Status Code no es OK es: "+scraping.getStatusConnectionCode());
    }
    private Tiempo cargarTiempoProvincia(Provincia p) {
    	scraping.setUrlInicio(p.getHref());
		// Compruebo si el protocolo http es 200
    	Tiempo t_provincia;
        if (scraping.getStatusConnectionCode() == 200) {
    		Document document = scraping.getHtmlDocument();// Obtengo el HTML de la web en un objeto Document
    		Elements tabla = document.select("table.tabla-horas");
    		Elements registros = tabla.select("table > tbody > tr");
    		//System.out.println(registros.toString()+"\n-------------------------------------------------");
    		Calendar c=Calendar.getInstance();
    		Element tiempo_prov=null;
    		int i=0,hora = c.get(Calendar.HOUR_OF_DAY),minuto = c.get(Calendar.MINUTE),
    				temperatura,v_viento,precipitacion;
    		String clima, d_viento;
    		//System.out.println(hora+":"+minuto); 
    		hora++;//se considera que para el minuto 50 interesa más la información de la siguiete hora
    		while (i<registros.size()) {
    			tiempo_prov = registros.get(i);
    			i++;
    			if (tiempo_prov.selectFirst("td>span.hora:contains("+hora+":00)")!=null) {
    				i=registros.size();
    			}
    		}
    		hora--;
    		clima = tiempo_prov.select("td.descripcion>strong").text();
    		temperatura = Integer.parseInt(tiempo_prov.select("td.temperatura").text().substring(0, 1));
    		d_viento = tiempo_prov.select("td span.datos-viento>strong").text();
    		v_viento = Integer.parseInt(tiempo_prov.select("td span.datos-viento>span:contains(km/h)").text().substring(0, 1));
    		precipitacion = 0;
    		t_provincia = new Tiempo(hora, precipitacion, temperatura, v_viento, clima, d_viento);
		}else {
            System.out.println("El Status Code no es OK es: "+scraping.getStatusConnectionCode());
            t_provincia = null;
		}
        return t_provincia;
	}
    private class EnviarSemanal extends TickerBehaviour {
        public EnviarSemanal(Agent agente, long tiempo) {
            super(agente, tiempo);
        }
 
        public void onTick() {
            //Se crea y rellena el mensaje con la información que desea enviar.
            ACLMessage mensaje = new ACLMessage(ACLMessage.INFORM);
            Provincia p_solicitada;
            //Se envía un mensaje a cada suscriptor
            for (Subscription suscripcion:AgenteTiempoCom.this.suscripciones) {
            	try {
					p_solicitada = l_provincias.get(l_provincias.lastIndexOf((Object)suscripcion.getMessage().getContentObject()));
					mensaje.setContentObject(cargarTiempoProvincia(p_solicitada));
					suscripcion.notify(mensaje);
				} catch (UnreadableException e) {
					System.out.println(AgenteTiempoCom.this.getLocalName()+":\n\tERROR--->"+e.getMessage());
				}catch (IOException e) {
					System.out.println(AgenteTiempoCom.this.getLocalName()+":\n\tERROR--->"+e.getMessage());
				}
            }
        }
    }
 
    private class HacerSuscripcion extends SubscriptionResponder {
        private Subscription suscripcion;
 
        public HacerSuscripcion(Agent agente, MessageTemplate plantilla, SubscriptionManager gestor) {
            super(agente, plantilla, gestor);
        }
 
        //Método que maneja la suscripcion
 
        protected ACLMessage handleSubscription(ACLMessage propuesta)
                throws NotUnderstoodException {
            System.out.printf("%s: \n\tSUSCRIBE recibido de %s.\n",
            		AgenteTiempoCom.this.getLocalName(), propuesta.getSender().getLocalName());
            try {
				System.out.printf("%s: \n\tLa propuesta es: %s.\n",
						AgenteTiempoCom.this.getLocalName(), propuesta.getContentObject());
			} catch (UnreadableException e1) {
				System.out.println(AgenteTiempoCom.this.getLocalName()+"\n\tERROR--->"+e1.getMessage());
			}
            
          //De primeras suponemos que se rechaza la peticion
		    ACLMessage confirmacion = propuesta.createReply();
		    confirmacion.setPerformative(ACLMessage.REFUSE);
            
            //Comprueba los datos de la propuesta
            try {
				if (AgenteTiempoCom.this.compruebaMensaje(propuesta.getContentObject())) {
 
				    //Crea la suscripcion
				    this.suscripcion = this.createSubscription(propuesta);
 
				    try {
				        //El SubscriptionManager registra la suscripcion
				        this.mySubscriptionManager.register(suscripcion);
				        //Acepta la propuesta y la envía
					    confirmacion.setPerformative(ACLMessage.AGREE);
				    } catch (Exception e) {
				        System.out.println(AgenteTiempoCom.this.getLocalName() + ": Error en el registro de la suscripción.");
				    }
				}
			} catch (UnreadableException e) {
				System.out.println(AgenteTiempoCom.this.getLocalName()+"\n\tERROR-->"+e.getMessage());
			}
            return confirmacion;
        }
 
        //Maneja la cancelación de la suscripcion
 
        protected ACLMessage handleCancel(ACLMessage cancelacion) {
            System.out.printf("%s: CANCEL recibido de %s.\n",
            		AgenteTiempoCom.this.getLocalName(), cancelacion.getSender().getLocalName());
            try {
                //El SubscriptionManager elimina del registro la suscripcion
                this.mySubscriptionManager.deregister(this.suscripcion);
            } catch (Exception e) {
                System.out.println(AgenteTiempoCom.this.getLocalName() + ": Error en el desregistro de la suscripción.");
            }
            //Acepta la cancelación y responde
            ACLMessage cancela = cancelacion.createReply();
            cancela.setPerformative(ACLMessage.INFORM);
            return cancela;
        }
    }
}
