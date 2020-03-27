package com.scraping.cliente;

import java.io.IOException;

import com.scraping.domain.Provincia;
import com.scraping.domain.Tiempo;

import jade.core.AID;
import jade.core.Agent;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import jade.proto.SubscriptionInitiator;
 
public class AgenteConsumidor extends Agent {
 
	private Tiempo tiempoMeteo,tiempoCom;
	private SuscribirMeteo submeteo;
	private SuscribirTiempoCom subtiempoCom;
	private AID idMeteo,idTiempoCom;
	private boolean refuseT,refuseM;
    protected void setup() {
    	tiempoMeteo=null;tiempoCom =null;//se inicializan a null
    	refuseT=false;
    	refuseM=false;
    	Provincia provincia;
    	Object[] args = getArguments();
        if (args != null && args.length == 1) {
	    	 provincia = new Provincia ((String) args[0]);//Provincia a observar
	    	//Destinatarios
	    	idMeteo = new AID();
	        idMeteo.setLocalName("Meteosat");
	        idTiempoCom = new AID();
	        idTiempoCom.setLocalName("TiempoCom");
	        //Se crea un mensaje de tipo SUBSCRIBE y se asocia al protocolo FIPA-Subscribe.
	        ACLMessage mensajeMeteo = new ACLMessage(ACLMessage.SUBSCRIBE);
	        mensajeMeteo.setProtocol(FIPANames.InteractionProtocol.FIPA_SUBSCRIBE);
	        ACLMessage mensajeTiempo = new ACLMessage(ACLMessage.SUBSCRIBE);
	        mensajeTiempo.setProtocol(FIPANames.InteractionProtocol.FIPA_SUBSCRIBE);
	        submeteo = new SuscribirMeteo(this, mensajeMeteo);
	        subtiempoCom = new SuscribirTiempoCom(this, mensajeTiempo);
	        try {
	        	mensajeMeteo.setContentObject(provincia);
	        	mensajeMeteo.addReceiver(idMeteo);
	        	mensajeTiempo.setContentObject(provincia);
	        	mensajeTiempo.addReceiver(idTiempoCom);
		        this.addBehaviour(submeteo);
		        this.addBehaviour(subtiempoCom);
			} catch (IOException e) {
				System.out.println(AgenteConsumidor.this.getLocalName()+"\n\tERROR-->"+e.getMessage());
			}
        }
    }
    protected void takeDown() {
    	if(subtiempoCom!=null && !refuseT) {
    		subtiempoCom.cancel(idTiempoCom, false);
    		subtiempoCom.cancellationCompleted(idTiempoCom);
    	}
    	if(submeteo!=null && !refuseM) {
    		submeteo.cancel(idMeteo, false);
    		submeteo.cancellationCompleted(idMeteo);
    	}
    	System.out.println("Finalizando Agente");
    }
 
    private class SuscribirMeteo extends SubscriptionInitiator {
        private int cant_Actualizaciones;
        public SuscribirMeteo(Agent agente, ACLMessage mensaje) {
            super(agente, mensaje);
            cant_Actualizaciones=0;
        }
 
        //Maneja la respuesta en caso que acepte: AGREE
 
        protected void handleAgree(ACLMessage inform) {
            System.out.println(AgenteConsumidor.this.getLocalName() + ": Solicitud aceptada.");
        }
 
        // Maneja la respuesta en caso que rechace: REFUSE
 
        protected void handleRefuse(ACLMessage inform) {
        	refuseM=true;
            System.out.println(AgenteConsumidor.this.getLocalName() + ": Solicitud rechazada. Meteosat");
            myAgent.doDelete();
        }
 
        //Maneja la informacion enviada: INFORM
 
        protected void handleInform(ACLMessage inform) {
        	Tiempo t_provincia = null;
        	this.cant_Actualizaciones++;
            if (this.cant_Actualizaciones >= 10) {
                this.cancel(inform.getSender(), false);
                this.cancellationCompleted(inform.getSender());
                submeteo=null;
                myAgent.doDelete();
            }else {
	            try {
					t_provincia = (Tiempo) inform.getContentObject();
				} catch (UnreadableException e) {
					System.out.println(AgenteConsumidor.this.getLocalName() + "\n\tERROR-->"+e.getMessage());
				}
	            if(t_provincia!=null) {
		            tiempoMeteo = t_provincia;
		            System.out.println(AgenteConsumidor.this.getLocalName() + ":\n>-Meteosat"+tiempoMeteo.toString()+"-<");
	            }
	            if(tiempoCom!=null && tiempoCom!=null) {
	            	if(tiempoMeteo.getHora()==tiempoCom.getHora() && !tiempoMeteo.equals(tiempoCom)) {
	            		System.out.println(AgenteConsumidor.this.getLocalName()+"\n\tMeteoSat discrepa de Tiempo.com en temperatura o velocidad del viento");
	            	}
	            }
            }
        }
 
        //Maneja la respuesta en caso de fallo: FAILURE
 
        protected void handleFailure(ACLMessage failure) {
            //Se comprueba si el fallo viene del AMS o de otro agente.
            if (failure.getSender().equals(myAgent.getAMS())) {
                System.out.println(AgenteConsumidor.this.getLocalName() + ": El destinatario no existe.");
            } else {
                System.out.printf("%s: El agente %s falló al intentar realizar la acción solicitada.\n",
                		AgenteConsumidor.this.getLocalName(), failure.getSender().getName());
            }
            refuseM=true;
            myAgent.doDelete();
        }
 
        public void cancellationCompleted(AID agente) {
            //Creamos una plantilla para solo recibir los mensajes del agente que va a cancelar la suscripción
            MessageTemplate template = MessageTemplate.MatchSender(agente);
            ACLMessage msg = blockingReceive(template);
 
            //Comprobamos que tipo de mensaje llegó: INFORM o FAILURE
            if (msg.getPerformative() == ACLMessage.INFORM)
                System.out.printf("%s : Suscripcion cancelada con el agente %s.\n",
                		AgenteConsumidor.this.getLocalName(), agente.getLocalName());
            else
                System.out.printf("%s: Se ha producido un fallo en la cancelación con el agente %s.\n",
                		AgenteConsumidor.this.getLocalName(), agente.getLocalName());
        }
    }
    private class SuscribirTiempoCom extends SubscriptionInitiator {
    	private int cant_Actualizaciones;
        public SuscribirTiempoCom(Agent agente, ACLMessage mensaje) {
            super(agente, mensaje);
            cant_Actualizaciones=0;
        }
 
        //Maneja la respuesta en caso que acepte: AGREE
 
        protected void handleAgree(ACLMessage inform) {
            System.out.println(AgenteConsumidor.this.getLocalName() + ": Solicitud aceptada.");
        }
 
        // Maneja la respuesta en caso que rechace: REFUSE
 
        protected void handleRefuse(ACLMessage inform) {
        	refuseT=true;
            System.out.println(AgenteConsumidor.this.getLocalName() + ": Solicitud rechazada. TiempoCom");
            myAgent.doDelete();
        }
 
        //Maneja la informacion enviada: INFORM
 
        protected void handleInform(ACLMessage inform) {
        	Tiempo t_provincia = null;
        	this.cant_Actualizaciones++;
            if (this.cant_Actualizaciones >= 10) {
                this.cancel(inform.getSender(), false);
                this.cancellationCompleted(inform.getSender());
                subtiempoCom=null;
                myAgent.doDelete();
            }else {
	            try {
					t_provincia = (Tiempo) inform.getContentObject();
				} catch (UnreadableException e) {
					System.out.println(AgenteConsumidor.this.getLocalName() + "\n\tERROR-->"+e.getMessage());
				}
	            if(t_provincia!=null) {
	            	tiempoCom = t_provincia;
		            System.out.println(AgenteConsumidor.this.getLocalName() + ":\n>-Tiempo.com"+tiempoCom.toString()+"-<");
	            }
	            if(tiempoCom!=null && tiempoMeteo!=null) {
	            	if(tiempoMeteo.getHora()==tiempoCom.getHora() && !tiempoCom.equals(tiempoMeteo)) {
	            		System.out.println(AgenteConsumidor.this.getLocalName()+"\n\tTiempo.com discrepa de Meteosat en temperatura o velocidad del viento");
	            	}
	            }
            }
        }
 
        //Maneja la respuesta en caso de fallo: FAILURE
 
        protected void handleFailure(ACLMessage failure) {
            //Se comprueba si el fallo viene del AMS o de otro agente.
            if (failure.getSender().equals(myAgent.getAMS())) {
                System.out.println(AgenteConsumidor.this.getLocalName() + ": El destinatario no existe.");
            } else {
                System.out.printf("%s: El agente %s falló al intentar realizar la acción solicitada.\n",
                		AgenteConsumidor.this.getLocalName(), failure.getSender().getName());
            }
            refuseT=true;
            myAgent.doDelete();
        }
 
        public void cancellationCompleted(AID agente) {
            //Creamos una plantilla para solo recibir los mensajes del agente que va a cancelar la suscripción
            MessageTemplate template = MessageTemplate.MatchSender(agente);
            ACLMessage msg = blockingReceive(template);
 
            //Comprobamos que tipo de mensaje llegó: INFORM o FAILURE
            if (msg.getPerformative() == ACLMessage.INFORM)
                System.out.printf("%s : Suscripcion cancelada con el agente %s.\n",
                		AgenteConsumidor.this.getLocalName(), agente.getLocalName());
            else
                System.out.printf("%s: Se ha producido un fallo en la cancelación con el agente %s.\n",
                		AgenteConsumidor.this.getLocalName(), agente.getLocalName());
        }
    }
}

