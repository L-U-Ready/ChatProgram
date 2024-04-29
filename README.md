# LeeYuJoon Chatting Program

## 구현 기능 설명입니다.
---
### 1. ChatServer.java

#### ChatServer class
```java
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
```
- `portNum`
  
`portNum = 12345`로 설정하고, `serverSocket`에 `portnum`으로 설정해놨습니다. `portNum`은 이후 수정 불가능하게 하고, 개발자가 변경하기 편하도록 `static final`로 상단에 선언해두었습니다.

- `chatClients`와 `channelMap`

`chatClients`와 `channelMap` 모두 `Map`으로 선언해두었습니다.

  - **`chatClients`**
    - `Map<String, PrintWriter> chatClients = new HashMap<>();`
      - String -> 사용자의 id
      - PrintWriter -> 사용자의 입력
  - **`channelMap`**
  - `Map<Integer, Set<String>> channelMap = new TreeMap<>();`
    - Integer -> 채널 방 번호
    - Set<String> -> 채널 번호마다 들어있는 사용자의 id
    - TreeMap -> 이후 `/list`를 통해 생성된 채널들을 출력할 때 오름차순으로 보여주기 위함.

  - channelMap.put(-1, new HashSet<>()) -> Main Channel을 -1로 설정하기 위해 Server가 실행될 때 -1을 put합니다.

- `Socket socket = serverSocket.accept();`
  - serverSocket에 accept가 들어오면, `new ChatServerThread`를 생성하고, `socket, chatClients, channelMap)을 넣어줍니다.

#### ChatServerThread
**생성자**

사용자의 id를 관리합니다.

`chatClients`에 이미 있는 id라면, 재입력을 요구하고, id가 중복되지 않았을 때, `chatClients`에 id와 PrintWriter를 put합니다. 

서버에 사용자의 IP와 id의 사용자가 접속했다는 문구를 출력합니다.

**@Override run() 메소드**

switch 문을 통해 명령어와 사용자의 채팅 출력을 담당합니다.

String msg = in.readLine()으로, 사용자의 입력을 msg에 할당합니다.
```java
String[] message = msg.split(" ");
String order = message[0];
```
으로 사용자의 명령을 order에 할당합니다.
이후 채널 번호는 channelNum = parseInt(message[1]);로 channelNum에 할당합니다. (기본 채널은 private int channelNum = -1;로 필드에 선언되어 있습니다.)
1. case "/list"

채널 번호들이 key값으로 저장된 channelMap에 keySet()을 통해 조회해 출력하고, key == -1인 경우에 main을 출력합니다.

2. case "/create"


