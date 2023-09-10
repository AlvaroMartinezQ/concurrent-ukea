/*

	Author: Alvaro Martinez Quiroga
	Contact: alvaroo2302@gmail.com
	Github: github.com/AlvaroMartinezQ

*/

package ukea;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class Personal {
	/*
	 * Contiene el ciclo de vida de los procesos
	 */
	
	private static final int NUM_CLIENTES = 8;
	private static final int NUM_EMP_COGE_PEDIDOS = 4;
	private static final int NUM_EMP_ELABORA_PRODUCTOS = 3;
	private static final int NUM_EMP_PREPARA_PEDIDO = 3;
	private static final int NUM_DISPENSADORES = 2;
	
	private final int N_THREADS = NUM_CLIENTES + NUM_EMP_COGE_PEDIDOS + NUM_EMP_ELABORA_PRODUCTOS + NUM_EMP_PREPARA_PEDIDO;
	private ExecutorService executor = Executors.newFixedThreadPool(N_THREADS);
	
	private static Ukea ukea = new Ukea(NUM_DISPENSADORES);
	
	private static final int MIN_PRODUCTOS = 1;
	private static final int MAX_PRODUCTOS = 5;
	
	private static final int velocidad_llegada = 10000;
	
	private static AtomicBoolean tiendaAbierta = new AtomicBoolean();
	private static AtomicInteger hora = new AtomicInteger(0);
	
	/*
	 * Si el turno es false trabajara el personal con id par (incluye el 0)
	 * Si el turno es true trabajara el personal con id impar
	 */
	private static AtomicBoolean turno = new AtomicBoolean();
	
	/* 
	 * Simulacion de horas - 48 segundos = 1 dia
	 * Tienda abierta 14 horas, turnos de 7 horas
	 */
	private static AtomicInteger hilosEsperandoApertura = new AtomicInteger();
	private static Semaphore esperaApertura = new Semaphore(0);
	
	private static AtomicInteger hilosEsperandoTrabajar = new AtomicInteger();
	private static Semaphore esperaTrabajar = new Semaphore(0);
	
	// ---------------- Metodos procesos ----------------- //
	
	public static void cliente(int id) {
		System.out.println("Cliente " + id + " activo.");
		int rand = 0;
		boolean especial = false;
		double dinero = 300.00;
		// vivir - ir a la tienda (crear pedido)
		while(true) {
			try {
				if(tiendaAbierta.get()) {
					// Puede el cliente hacer pedido
					rand = ThreadLocalRandom.current().nextInt(MIN_PRODUCTOS, MAX_PRODUCTOS + 1);
					especial = ThreadLocalRandom.current().nextBoolean();
					Pedido p = new Pedido(rand, especial);
					double precio = ukea.relizarPedido(p, id, dinero);
					double comprobante = (int) precio;
					if(comprobante == -1) {
						// No lo puede pagar, devolverlo
						ukea.devolverPedido(p);
					} else if (comprobante == -2){
						// Se ha abortado la operacion por falta de existencias, no hacer nada
					} else {
						// Lo puede pagar, actualizar dinero del cliente en concreto
						dinero = dinero - precio;
					}
				} else {
					//System.out.println("El cliente: " + id + " no puede realizar pedidos, tienda cerrada.");
					hilosEsperandoApertura.incrementAndGet();
					esperaApertura.acquire();
				}
				Thread.sleep((long)(Math.random() * velocidad_llegada));
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static void empCogePedidos(int id) {
		System.out.println("empCogePedidos " + id + " activo.");
		/*
		 * Acciones: vivir - trabajar (cuando le toque)
		 * Ciclo de vida en trabajo:
		 * 		1. Entregar pedidos a clientes (si hay)
		 * 		2. Coger pedidos de cliente (si hay)
		 * 		3. Pasar pedidos a listas de produccion (si hay)
		 */
		while(true) {
			try {
				if(tiendaAbierta.get()) {
					// Puede trabajar
					if((id % 2 == 0 && !turno.get()) || (id % 2 != 0 && turno.get())) {
						//System.out.println("empCogePedidos: " + id + " esta en su turno de trabajo.");
						ukea.entregaPedido(id);
						ukea.pedidoAProduccion(id);
					} else {
						//System.out.println("empCogePedidos: " + id + " no esta en su turno de trabajo.");
						hilosEsperandoTrabajar.incrementAndGet();
						esperaTrabajar.acquire();
					}
				} else {
					// Tienda cerrada
					//System.out.println("empCogePedidos: " + id + " tienda cerrada.");
					hilosEsperandoApertura.incrementAndGet();
					esperaApertura.acquire();
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static void empElaboraProductos(int id) {
		System.out.println("empElaboraProductos " + id + " activo.");
		/*
		 * Acciones: vivir - trabajar (cuando le toque)
		 * Ciclo de vida en trabajo:
		 * 		1. Elaborar pedidos especiales (si hay)
		 */
		while(true) {
			try {
				if(tiendaAbierta.get()) {
					// Puede trabajar
					if((id % 2 == 0 && !turno.get()) || (id % 2 != 0 && turno.get())) {
						//System.out.println("empElaboraProductos: " + id + " esta en su turno de trabajo.");
						ukea.preparaEspecial(id);
					} else {
						//System.out.println("empElaboraProductos: " + id + " no esta en su turno de trabajo.");
						hilosEsperandoTrabajar.incrementAndGet();
						esperaTrabajar.acquire();
					}
				} else {
					// Tienda cerrada
					//System.out.println("empElaboraProductos: " + id + " tienda cerrada.");
					hilosEsperandoApertura.incrementAndGet();
					esperaApertura.acquire();
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static void empPreparaPedido(int id) {
		System.out.println("empPreparaPedido " + id + " activo.");
		/*
		 * Acciones: vivir - trabajar (cuando le toque)
		 * Ciclo de vida en trabajo:
		 * 		1. Si hay productos en lina de listos especiales pasarlos a linea principal para entregarlos
		 * 		2. Elaborar pedidos normales (si hay)
		 */
		while(true) {
			try {
				if(tiendaAbierta.get()) {
					// Puede trabajar
					if((id % 2 == 0 && !turno.get()) || (id % 2 != 0 && turno.get())) {
						//System.out.println("empPreparaPedido: " + id + " esta en su turno de trabajo.");
						// Trabaja el personal
						ukea.cambiaLineaEspecial(id);
						ukea.preparaNormal(id);
					} else {
						//System.out.println("empPreparaPedido: " + id + " no esta en su turno de trabajo.");
						hilosEsperandoTrabajar.incrementAndGet();
						esperaTrabajar.acquire();
					}
				} else {
					// Tienda cerrada
					//System.out.println("empPreparaPedido: " + id + " tienda cerrada.");
					hilosEsperandoApertura.incrementAndGet();
					esperaApertura.acquire();
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}		
	}
	
	public static void encargado() {
		System.out.println("Encargado activo.");
		// vivir - abrir tienda - trabajar (siempre) - cerrar tienda
		int tiempo = 0;
		while(true) {
			try {
				Thread.sleep(2000); // Simulacion de 1 hora como 2 segundos
				tiempo = hora.incrementAndGet();
				System.out.println("\n  Tiempo: " + tiempo + ":00  \n");
				if(tiempo == 6) {
					// Hora de abrir
					tiendaAbierta.set(true);
					System.out.println("\nEncargado: tienda abierta.\n");
					if(hilosEsperandoApertura.get() > 0) {
						esperaApertura.release(hilosEsperandoApertura.get());
						hilosEsperandoApertura.set(0);
					}
					if(hilosEsperandoTrabajar.get() > 0) {
						esperaTrabajar.release(hilosEsperandoTrabajar.get());
						hilosEsperandoTrabajar.set(0);
					}
				} else if (tiempo == 13) {
					// Primer cambio de turno
					turno.set(true);
					System.out.println("\nEncargado: cambio de turno.\n");
					if(hilosEsperandoTrabajar.get() > 0) {
						esperaTrabajar.release(hilosEsperandoTrabajar.get());
						hilosEsperandoTrabajar.set(0);
					}
				} else if (tiempo == 20) {
					// Hora de cerrar
					tiendaAbierta.set(false);
					turno.set(false);
					System.out.println("\nEncargado: tienda cerrada. Si hay pedidos realizandose han de entregarse ahora.\n");
					if(hilosEsperandoApertura.get() > 0) {
						esperaApertura.release(hilosEsperandoApertura.get());
						hilosEsperandoApertura.set(0);
					}
					if(hilosEsperandoTrabajar.get() > 0) {
						esperaTrabajar.release(hilosEsperandoTrabajar.get());
						hilosEsperandoTrabajar.set(0);
					}
				} else if (tiempo == 24) {
					// Nuevo dia, imprimo estado de tienda
					ukea.infoTienda();
					hora.set(0);
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void exec() {
		executor.submit(() -> encargado());
		for(int i = 0; i <  NUM_EMP_COGE_PEDIDOS; i++) {
			int id = i;
			executor.submit(() -> empCogePedidos(id));
		}
		for(int i = 0; i <  NUM_EMP_ELABORA_PRODUCTOS; i++) {
			int id = i;
			executor.submit(() -> empElaboraProductos(id));
		}
		for(int i = 0; i <  NUM_EMP_PREPARA_PEDIDO; i++) {
			int id = i;
			executor.submit(() -> empPreparaPedido(id));
		}
		for(int i = 0; i < NUM_CLIENTES ; i++) {
			int id = i;
			executor.submit(() -> cliente(id));
		}
		try {
			executor.awaitTermination(30, TimeUnit.MINUTES);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}
	
	// ------------------------- Main -------------------------
	
	public static void main(String[] args) {
		//Creacion de hilos que realizan llamadas a los metodos correspondientes
		System.out.println("\n\n***    UKEA    ***\n\n");
		new Personal().exec();
	}
	
}
