package org.example;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

import static java.lang.Integer.parseInt;

public class ChatServer {
    static final int portNum = 12345;

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(portNum)) {
            System.out.println("서버가 준비되었습니다.");

            Map<String, PrintWriter> chatClients = new HashMap<>();
            Map<Integer, Set<String>> channelMap = new TreeMap<>();
            channelMap.put(-1, new HashSet<>());

            while (true) {
                Socket socket = serverSocket.accept();
                new ChatServerThread(socket, chatClients, channelMap).start();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

class ChatServerThread extends Thread {
    private Socket socket;
    private String id;
    private String userIP;
    private Map<String, PrintWriter> chatClients;
    private Map<Integer, Set<String>> channelMap;
    private BufferedReader in;
    private PrintWriter out;
    private int channelNum = -1;

    public ChatServerThread(Socket socket, Map<String, PrintWriter> chatClients, Map<Integer, Set<String>> channelMap) {
        this.socket = socket;
        this.chatClients = chatClients;
        this.channelMap = channelMap;

        try {
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            id = in.readLine();
            userIP = in.readLine();
            synchronized (chatClients) {
                while(true) {
                    if (chatClients.containsKey(this.id)) {
                        out.println("[System]");
                        out.println("이미 사용 중인 id입니다. 다시 입력해주세요 : ");
                        id = in.readLine();
                    } else {
                        chatClients.put(this.id, out);
                        Set<String> mainChannel = channelMap.get(channelNum);
                        mainChannel.add(this.id);
                        help();
                        System.out.println(id + " 닉네임의 사용자가 연결했습니다.");
                        System.out.println(id + " 의 IP : " + userIP);
                        break;
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void run() {
        System.out.println(id + " 님의 채팅");
        String msg = null;
        try {
            while ((msg = in.readLine()) != null) {
                String[] message = msg.split(" ");
                String order = message[0];
                switch (order) {
                    case "/list":
                        System.out.println(id + " 님이 " + "채널 목록을 조회하셨습니다.");
                        out.println("[System]");
                        for (Integer channel : channelMap.keySet()) {
                            if(channel == -1) out.println("main 채널");
                            else out.println(channel + " 번 채널");

                        }
                        break;
                    case "/create":
                        channelNum = parseInt(message[1]);
                        if (!channelMap.containsKey(channelNum)) {
                            channelMap.put(channelNum, new HashSet<>());
                            out.println("[System]");
                            out.println("채널 " + channelNum + " 생성 완료"); // 요청을 보낸 client한테 전송
                            System.out.println(id + " 님이 " + channelNum + " 번 방을 생성하셨습니다."); // 서버에 출력
                        } else {
                            out.println("이미 존재하는 채널입니다."); // 요청을 보낸 client한테 전송
                        }
                        channelNum = -1;
                        break;
                    case "/join":
                        //일단 main channel에서 client 없애야 함.
                        if(channelMap.containsKey(channelNum)){
                            Set<String> mainChannel = channelMap.get(channelNum);
                            mainChannel.remove(id);
                        }
                        // channelNum 번호 새로 저장
                        channelNum = parseInt(message[1]);
                        // channelNum으로 방 이동
                        if (channelMap.containsKey(channelNum)) {
                            Set<String> channelClients = channelMap.get(channelNum);
                            channelClients.add(id);
                            out.println("[System]");
                            out.println("채널 " + channelNum + "에 입장하였습니다."); // 요청을 보낸 client한테 전송
                            System.out.println(id + " 님이 " + channelNum + " 번 방에 입장하셨습니다.");
                            // 방에 입장한 사용자들에게 입장 메시지를 전달
                            for (String client : channelClients) {
                                if (!client.equals(id)) {
                                    chatClients.get(client).println(id + " 님이 방에 입장하셨습니다."); // 요청을 보낸 client 빼고 모두에게 전송
                                }
                            }
                        } else {
                            out.println("존재하지 않는 채널입니다."); // 요청을 보낸 client한테 전송
                        }
                        break;
                    case "/exit":
                        if (channelNum == -1){
                            out.println("[System]");
                            out.println("main 채널에서는 퇴장할 수 없습니다.");
                            out.println("접속을 종료하려면 /bye를 이용해주세요.");
                            break;
                        } else {
                            if (channelMap.containsKey(channelNum)) {
                                Set<String> channelClients = channelMap.get(channelNum);
                                if (channelClients.contains(id)) {
                                    channelClients.remove(id);
                                    System.out.println(id + " 님이 " + channelNum + " 번 방에서 퇴장하셨습니다.");
                                    out.println("[System]");
                                    out.println(channelNum + " 번 방에서 퇴장하셨습니다.");
                                    if (channelClients.isEmpty()) {
                                        channelMap.remove(channelNum);
                                        System.out.println(channelNum + " 번 방이 삭제되었습니다.");
                                    } else {
                                        for (String client : channelClients) {
                                            if (!client.equals(id)) {
                                                chatClients.get(client).println(id + " 님이 방을 나갔습니다.");
                                            }
                                        }
                                    }
                                } else {
                                    out.println("[System]");
                                    out.println("오류: 해당 사용자가 채널에 속해있지 않습니다.");
                                    System.out.println("오류: 해당 사용자가 채널에 속해있지 않습니다.");
                                }
                            } else {
                                out.println("[System]");
                                out.println("오류: 해당 채널이 존재하지 않습니다.");
                                System.out.println("오류: 해당 채널이 존재하지 않습니다.");
                            }
                        }
                        // 사용자를 메인 채널로 이동시킴
                        channelNum = -1; // 메인 채널의 채널 번호 또는 다른 채널의 채널 번호로 설정
                        System.out.println(id + " 님이 메인 채널로 이동하셨습니다.");
                        if(channelMap.containsKey(channelNum)){
                            Set<String> mainChannel = channelMap.get(channelNum);
                            mainChannel.add(id);
                        }
                        break;
                    case "/users":
                        users();
                        break;
                    case "/roomusers":
                        roomUsers(channelNum);
                        break;
                    case "/whisper":
                        whisper(msg);
                        break;
                    case "/help":
                        help();
                        break;
                    case "/bye":
                        bye();
                        break;
                    default:
                        // 해당 채널에 속한 모든 사용자들에게 메시지 전송
                        sendMessage(id + " : " + msg, channelNum);
                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
    public void sendMessage(String msg, int cn){ //cn : channelNum
        if (channelMap.containsKey(cn)) {
            Set<String> channelClients = channelMap.get(cn);
            for (String client : channelClients) {
                chatClients.get(client).println(msg);
            }
        }

    }
    public void users(){
        out.println(chatClients.keySet());
    }
    public void roomUsers(int cn){
        if (channelMap.containsKey(cn)) {
            Set<String> channelClients = channelMap.get(cn);
            out.println(channelClients);
        }
    }
    public void whisper(String msg){
        int firstSpaceIndex = msg.indexOf(" ");
        if (firstSpaceIndex == -1) return; //공백이 없다면....

        int secondSpaceIndex = msg.indexOf(" ", firstSpaceIndex + 1);
        if (secondSpaceIndex == -1) return; //두번재 공백이 없다는 것도 메시지가 잘못된거니까..

        String to = msg.substring(firstSpaceIndex + 1, secondSpaceIndex);
        String message = msg.substring(secondSpaceIndex + 1);

        //to(수신자)에게 메시지 전송.
        PrintWriter pw = chatClients.get(to);
        if (pw != null) {
            pw.println(id + "님으로부터 온 비밀 메시지 : " + message);
        } else {
            out.println("[System]");
            out.println("오류 : 수신자 " + to + " 님을 찾을 수 없습니다.");
        }
    }

    public void bye() {
        synchronized (chatClients) {
            chatClients.remove(id);
        }
        System.out.println(id + " 닉네임의 사용자가 연결을 끊었습니다.");
        sendMessage(id + " 님이 채팅에서 나갔습니다.", channelNum);
    }
    public void help(){
        out.println("[System]");
        out.println("방 목록 보기 : /list\n" +
                "방 생성 : /create\n" +
                "방 입장 : /join [방번호]\n" +
                "방 나가기 : /exit\n" +
                "접속종료 : /bye\n" +
                "사용자 목록 확인 : /users\n" +
                "현재 방에 있는 사용자 목록 확인 : /roomusers\n" +
                "귓속말 : /whisper [닉네임] [메세지]\n" +
                "명령어 모음 : /help\n");
    }
}