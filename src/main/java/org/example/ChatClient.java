package org.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

import static java.lang.Integer.parseInt;

public class ChatClient {
    static InetAddress ip = null;
    static int portNum = 12345;
    public static void main(String[] args) {
        try (

                Socket socket = new Socket(InetAddress.getLocalHost(), portNum);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                BufferedReader keyboard = new BufferedReader(new InputStreamReader(System.in));
        ) {
            ip = InetAddress.getLocalHost(); //Client의 IP 저장

            System.out.print("닉네임을 설정해주세요 : ");
            String nickName = keyboard.readLine();
            out.println(nickName); // 메인 서버에 닉네임 전송

            new InputThread(socket, in).start(); //네트워크에서 입력된 내용을 담당하는 부분을 Thread로..


            InetAddress localHost = InetAddress.getLocalHost();
            out.println(localHost.getHostAddress()); // 메인 서버에 IP 전송

            String userInput;

            while((userInput = keyboard.readLine()) != null) {

                out.println(userInput);
            }
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
class InputThread extends Thread {
    private Socket socket;
    private BufferedReader in;

    InputThread(Socket socket, BufferedReader in) {
        this.socket = socket;
        this.in = in;
    }

    @Override
    public void run() {
        try {
            String msg = null;
            while ((msg = in.readLine()) != null) {
                System.out.println(msg);
            }
        } catch (Exception e) {
            System.out.println(e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}