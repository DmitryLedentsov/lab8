package client;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.*;
import java.nio.ByteBuffer;

import collection.WorkerObservableManager;
import commands.ClientCommandManager;
import common.auth.User;
import common.connection.*;
import common.exceptions.*;
import common.io.OutputManager;
import io.OutputterUI;

import static common.io.ConsoleOutputter.print;
import static common.io.ConsoleOutputter.printErr;

/**
 * client class
 */
public class Client extends Thread implements SenderReceiver {
    private SocketAddress address;
    private DatagramSocket socket;
    public final int MAX_TIME_OUT = 500;
    public final int MAX_ATTEMPTS = 3;
    private User user;
    private User attempt;
    private boolean running;

    private ClientCommandManager commandManager;
    private OutputManager outputManager;
    private volatile boolean receivedRequest;
    private volatile boolean authSuccess;

    private boolean connected;
    private WorkerObservableManager collectionManager;
    public boolean isReceivedRequest(){
        return receivedRequest;
    }
    /**
     * initialize client
     *
     * @param addr Address
     * @param p port
     * @throws ConnectionException
     */
    private void init(String addr, int p) throws ConnectionException {
        connect(addr, p);
        running = true;
        connected = false;
        authSuccess = false;
        collectionManager = new WorkerObservableManager();
        commandManager = new ClientCommandManager(this);
        //setDaemon(true);
        setName("client thread");
    }

    public Client(String addr, int p) throws ConnectionException {
        init(addr, p);
    }

    public void setUser(User usr){
        user = usr;
    }
    public User getUser(){
        return user;
    }
    public void setAttemptUser(User u){
        attempt = u;
    }

    public User getAttemptUser() {
        return attempt;
    }

    /**
     * connects client to server
     *
     * @param addr Address
     * @param p port
     * @throws ConnectionException
     */
    public void connect(String addr, int p) throws ConnectionException {
        try {
            address = new InetSocketAddress(InetAddress.getByName(addr), p);
        } catch (UnknownHostException e) {
            throw new InvalidAddressException();
        } catch (IllegalArgumentException e) {
            throw new InvalidPortException();
        }
        try {
            socket = new DatagramSocket();
            socket.setSoTimeout(MAX_TIME_OUT);
        } catch (IOException e) {
            throw new ConnectionException("cannot open socket");
        }
    }

    /**
     * sends request to server
     *
     * @param request request
     * @throws ConnectionException
     */
    public void send(Request request) throws ConnectionException {
        try {
            //request.setStatus(Request.Status.SENT_FROM_CLIENT);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(BUFFER_SIZE);
            ObjectOutputStream objOutput = new ObjectOutputStream(byteArrayOutputStream);
            objOutput.writeObject(request);
            DatagramPacket requestPacket = new DatagramPacket(byteArrayOutputStream.toByteArray(), byteArrayOutputStream.size(), address);
            socket.send(requestPacket);
            byteArrayOutputStream.close();
        } catch (IOException e) {
            throw new ConnectionException("something went wrong while sending request");
        }
    }

    /**
     * receive message from server
     *
     * @return response
     * @throws ConnectionException
     * @throws InvalidDataException
     */
    public Response receive() throws ConnectionException, InvalidDataException {
        try {
            socket.setSoTimeout(MAX_TIME_OUT);
        } catch (SocketException ignored) {

        }
        ByteBuffer bytes = ByteBuffer.allocate(BUFFER_SIZE);
        DatagramPacket receivePacket = new DatagramPacket(bytes.array(), bytes.array().length);
        try {
            socket.receive(receivePacket);
        } catch (SocketTimeoutException e) {
            for (int attempts = MAX_ATTEMPTS; attempts > 0; attempts--) {
                //printErr("server response timeout exceeded, trying to reconnect. " + attempts + " attempts left");
                try {
                    socket.receive(receivePacket);
                    break;
                } catch (IOException ignored) {

                }
            }

            throw new ConnectionTimeoutException();
        } catch (IOException e) {
            throw new ConnectionException("something went wrong while receiving response");
        }

        try {
            ObjectInputStream objectInputStream = new ObjectInputStream(new ByteArrayInputStream(bytes.array()));
            return (Response) objectInputStream.readObject();
        } catch (ClassNotFoundException | ClassCastException | IOException e) {
            throw new InvalidReceivedDataException();
        }
    }

    private Response receiveWithoutTimeLimits() throws ConnectionException,InvalidDataException{
        try {
            socket.setSoTimeout(0);
        } catch (SocketException ignored) {

        }
        ByteBuffer bytes = ByteBuffer.allocate(BUFFER_SIZE);
        DatagramPacket receivePacket = new DatagramPacket(bytes.array(), bytes.array().length);
        try {
            socket.receive(receivePacket);
        } catch (IOException e) {
            throw new ConnectionException("something went wrong while receiving response");
        }

        try {
            ObjectInputStream objectInputStream = new ObjectInputStream(new ByteArrayInputStream(bytes.array()));
            return (Response) objectInputStream.readObject();
        } catch (ClassNotFoundException | ClassCastException | IOException e) {
            throw new InvalidReceivedDataException();
        }
    }

    /**
     * runs client until interrupt
     */
    @Override
    public void run() {
        Request hello = new CommandMsg();
        hello.setStatus(Request.Status.HELLO);
        try {
            send(hello);
        } catch (ConnectionException e) {
            printErr("cannot load collection from server");
        }
        while (running) {
            try {
                receivedRequest = false;
                Response response = receiveWithoutTimeLimits();

                switch (response.getStatus()) {
                    case COLLECTION:
                        collectionManager.applyChanges(response);
                        connected = true;
                        print("loaded!");
                        break;
                    case BROADCAST:
                        //commandManager.condition.await();
                        print("broadcast!");
                        collectionManager.applyChanges(response);
                        break;
                    case AUTH_SUCCESS:
                        user = attempt;
                        authSuccess = true;
                        break;
                    case ERROR:
                        outputManager.error(response.getMessage());
                    default:
                        print(response.getMessage());
                        receivedRequest = true;
                        break;
                }

            } catch (ConnectionException e) {

            } catch (InvalidDataException ignored) {

            }
        }
    }

    public void connectionTest(){
        connected = false;
        try {
            send(new CommandMsg().setStatus(Request.Status.CONNECTION_TEST));
            Response response = receive();

            connected= (response.getStatus()== Response.Status.FINE);
        } catch (ConnectionException| InvalidDataException ignored){

        }
    }
    public void processAuthentication(String login, String password, boolean register){
        attempt = new User(login,password);
        CommandMsg msg = new CommandMsg();
        if(register){
            msg = new CommandMsg("register").setStatus(Request.Status.DEFAULT).setUser(attempt);
        }
        else {
            msg = new CommandMsg("login").setStatus(Request.Status.DEFAULT).setUser(attempt);
        }
        try {
            send(msg);
            Response answer = receive();
            connected = true;
            authSuccess = (answer.getStatus() == Response.Status.AUTH_SUCCESS);
            if (authSuccess) {
                user = attempt;
            } else {
                outputManager.error("wrong passwd");
            }
        } catch (ConnectionTimeoutException e){
            outputManager.error("connection timeout");
            connected = false;
        } catch (ConnectionException|InvalidDataException e) {
            connected=false;
        }
    }
    public void consoleMode(){
        commandManager.consoleMode();
    }

    public boolean isConnected() {
        return connected;
    }

    public boolean isAuthSuccess(){
        return authSuccess;
    }
    public WorkerObservableManager getWorkerManager(){
        return collectionManager;
    }
    public ClientCommandManager getCommandManager(){return commandManager;}
    public OutputManager getOutputManager(){
        return outputManager;
    }
    public void setOutputManager(OutputManager out){
        outputManager = out;
    }
    /**
     * close client
     */
    public void close() {
        try {
            send(new CommandMsg().setStatus(Request.Status.EXIT));
        } catch (ConnectionException ignored){

        }
        running = false;
        commandManager.close();
        socket.close();
    }

}