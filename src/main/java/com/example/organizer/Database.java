package com.example.organizer;

import java.sql.Connection;        //DB 소켓같은것. 연결 표현하는 클래스
import java.sql.DriverManager;     //DB와 실제 연결을 만들어주는 클래스. 문열어주는 관리자 같은 것
import java.sql.SQLException;      //DB 작업 중 예외처리

public class Database {
    private static String DB_URL = "jdbc:sqlite:todo.db";    // Java Database Connectivity - 자바와 db가 통신 : 어떤db쓸건지 : 파일경로
}
