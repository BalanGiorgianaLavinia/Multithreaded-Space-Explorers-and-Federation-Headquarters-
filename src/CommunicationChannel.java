import java.util.concurrent.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;


public class CommunicationChannel {
	BlockingQueue<Message> messageFromHQs = new ArrayBlockingQueue<Message>(10000);
	BlockingQueue<Message> messageFromSpaceExplorers = new ArrayBlockingQueue<Message>(10000);
	ConcurrentHashMap<Long, ArrayBlockingQueue<Message>> tabela = new ConcurrentHashMap<>();

	Semaphore semPut = new Semaphore(1);	//semafor pentru thread curent
	Semaphore semPut2 = new Semaphore(1);	//semafor pentru pus mesaje pe canal
	Semaphore semGet = new Semaphore(1);	//semafor pentru thread curent


	public CommunicationChannel() {
	}

	
	public void putMessageSpaceExplorerChannel(Message message) {
		try{
			messageFromSpaceExplorers.put(message);
		}catch(Exception e){}
	}

	
	public Message getMessageSpaceExplorerChannel() {
		Message message = null;
		try{
			message = messageFromSpaceExplorers.take();
		}catch(Exception e){}

		return message;
	}


	public void putMessageHeadQuarterChannel(Message message) {

		if(message.getData().equals(HeadQuarter.EXIT) || 
			message.getData().equals(HeadQuarter.END) ){
				try{
					semPut2.acquire();
					messageFromHQs.put(message);
				} catch(Exception ex){}	

				semPut2.release();

				return;
			}
		
		long currentThread = Thread.currentThread().getId();

		try{
			semPut.acquire();

			if(!tabela.containsKey(currentThread)) {
				tabela.put(currentThread, new ArrayBlockingQueue<Message>(2));
			}
		
			if(tabela.get(currentThread).size() != 2) {
				tabela.get(currentThread).offer(message);
			}
		
			if(tabela.get(currentThread).size() == 2) {

				Message message1 = tabela.get(currentThread).remove();
				Message message2 = tabela.get(currentThread).remove();

				try{
					semPut2.acquire();
					messageFromHQs.put(message1);
					messageFromHQs.put(message2);
				}catch(Exception e2){}
				
				semPut2.release();
			}

		}catch(Exception e){}

		semPut.release();

	}

	
	public Message getMessageHeadQuarterChannel() {
		Message message = null;	
		Message message1 = null;
		Message message2 = null;

		try{
			semGet.acquire();
			try{
				//iau parintele sau mesajul de EXIT/END
				message1 = messageFromHQs.take();
				if(message1.getData().equals(HeadQuarter.EXIT) ||
					message1.getData().equals(HeadQuarter.END)){

						semGet.release();

						return message1;
				}
	
				//iau copilul
				message2 = messageFromHQs.take();
			}catch(Exception e){}
		}catch(Exception ex){}
		
		semGet.release();
		
		//construiesc un singur mesaj din cele doua
		message = new Message(message1.getCurrentSolarSystem(), message2.getCurrentSolarSystem(), message2.getData());

		return message;
	}
}



		