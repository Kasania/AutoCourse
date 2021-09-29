/*
 * Author : 나상혁 : Kasania
 * Filename : HSU
 * Desc :
 */
package com.kasania.core;

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class HSU {
    private String id;
    private String pw;

    public HSU(){
        List<String> idpw = new ArrayList<>();
        try {
            idpw = Files.readAllLines(Path.of("./idpw.txt"));
        } catch (IOException e) {
            System.out.println("ID/PW file not found.");
        }

        List<WebElement> elements;
        List<String> courseLinkURL = new ArrayList<>();
        List<Video> videoLinkURL = new LinkedList<>();
        WebDriver driver = null;
        Scanner scanner = new Scanner(System.in);
        try{
            driver = new ChromeDriver();

            if (idpw.size() == 0){
                System.out.println("ID : ");
                id = scanner.nextLine();
                System.out.println("PW : ");
                pw = scanner.nextLine();
                idpw.add(id);
                idpw.add(pw);
                Files.write(Path.of("./idpw.txt"),idpw, StandardCharsets.UTF_8, StandardOpenOption.CREATE);
                System.out.println("ID/PW file created.");
            }else{
                id = idpw.get(0);
                pw = idpw.get(1);
            }

            driver.get("https://learn.hansung.ac.kr/login.php");
            WebElement loginElem = driver.findElement(By.id("input-username"));
            loginElem.clear();
            loginElem.sendKeys(id);

            WebElement passwdElem = driver.findElement(By.id("input-password"));
            passwdElem.clear();
            passwdElem.sendKeys(pw);

            driver.findElement(By.xpath("//*[@id=\"region-main\"]/div/div/div/div[1]/div[1]/div[2]/form/div[2]/input")).click();

            elements = driver.findElements(By.className("course_label_re_02"));
            for (WebElement element : elements) {
                courseLinkURL.add(element.findElement(By.className("course_link")).getAttribute("href"));
            }

            if (courseLinkURL.size() > 0) {
                driver.get(courseLinkURL.get(0));
                if (!courseLinkURL.get(0).equals(driver.getCurrentUrl())) {
                    driver.findElement(By.id("btn-kakaoAuth")).click();
                    driver.findElement(By.id("btn-send-kakao")).click();
                    System.out.println("Please complete authentication.");
                    while (!courseLinkURL.get(0).equals(driver.getCurrentUrl())) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            break;
                        }
                    }
                }
            }

            thisWeek(courseLinkURL, videoLinkURL, driver, scanner);

        }catch(Exception e){
            e.printStackTrace();
            driver.close();
        }
        driver.close();
        System.out.println("Retv Compl");

    }

    public void normalPlayer(List<Video> videoLinkURL,WebDriver driver){
        for (Video v1 : videoLinkURL) {
            driver.get("https://learn.hansung.ac.kr/login.php");
            WebElement loginElem = driver.findElement(By.id("input-username"));
            loginElem.clear();
            loginElem.sendKeys(id);

            WebElement passwdElem = driver.findElement(By.id("input-password"));
            passwdElem.clear();
            passwdElem.sendKeys(pw);

            driver.findElement(By.xpath("//*[@id=\"region-main\"]/div/div/div/div[1]/div[1]/div[2]/form/div[2]/input")).click();

            driver.get(v1.url);
            driver.manage().timeouts().pageLoadTimeout(5, TimeUnit.SECONDS);
            try{
                driver.switchTo().alert().accept();
                driver.manage().timeouts().pageLoadTimeout(5, TimeUnit.SECONDS);
                driver.switchTo().alert().accept();
            }catch(NoAlertPresentException ignored){
            }
            try {
                synchronized (this){
                    this.wait(1000);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            driver.findElement(By.id("vod_player")).sendKeys("\n");

            try {
                synchronized (this){
                    this.wait(1000 * 5);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            String time = "";
            driver.findElement(By.id("vod_player")).sendKeys("\n");

            while(time.equals("")){
                time = driver.findElement(By.xpath("//*[@id=\"vod_player\"]/div[8]/div[4]/div[1]/span[1]")).getText();
            }
            System.out.println("Now playing : " + v1.name);
            System.out.println("Start from : " + time);
            driver.findElement(By.id("vod_player")).sendKeys("\n");
            int realtime = timeCheck(time);
            try {
                synchronized (this){
                    this.wait(1000L * (v1.second - realtime));
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
    }

    public void thisWeek(List<String> courseLinkURL, List<Video> videoLinkURL, WebDriver driver,Scanner scanner ){

        for (String courseLink : courseLinkURL) {
            driver.get(courseLink);
            String title = driver.findElement(By.xpath("//*[@id=\"page-header\"]/nav/div/div[3]/h1/a")).getText();

            WebElement content = driver.findElement(By.xpath("//*[@id=\"region-main\"]/div/div/div[2]/ul"))
                    .findElement(By.className("content"));

            List<WebElement> videos = content.findElements(By.className("activityinstance"));
            for (WebElement video : videos) {
                String url = "";
                try {
                    url = video.findElement(By.tagName("a")).getAttribute("href");
                } catch (NoSuchElementException ignored) {
                }

                if(url.contains("vod")){
                    url = url.replace("view","viewer");
                    String time = video.findElement(By.className("text-info")).getText().replace(", ","");
                    String name = video.findElement(By.className("instancename")).getText().split("\n")[0];
                    int realtime = timeCheck(time);
                    videoLinkURL.add(new Video(title +" : "+ name, url, realtime));
                }
            }
        }

        int ret = printVideoList(scanner, videoLinkURL);
        normalPlayer(videoLinkURL,driver);

    }

    private int timeCheck(String time) {
        String[] times = time.split(":");
        int realtime = 0;
        int timescale = 1;
        for (int i = times.length - 1; i >= 0; i--) {
            realtime += Integer.parseInt(times[i]) * timescale;
            timescale *= 60;
        }
        return realtime;
    }

    public int printVideoList(Scanner scanner, List<Video> videoLinkURL){
        while(true){

            System.out.println("1.start\n2.list\n3.remove");

            int input = scanner.nextInt();

            if(input == 1){
                return 1;
            }
            else if(input == 2){
                int i = 1;
                int total = 0;
                for (Video video : videoLinkURL) {
                    System.out.println(i++ + ". "+video.name + " , " + video.second+" sec");
                    total += video.second;
                }
                int hour = total/3600;
                int minute = (total-(3600*hour))/60;
                int second = (total- (3600*hour) - 60*minute);
                System.out.println("Total Playtime : "+hour +":" +minute + ":" +second + " ( "+ total/60 + " min )");
            }
            else if(input == 3){
                int i = 1;
                for (Video video : videoLinkURL) {
                    System.out.println(i++ + ". "+video.name + " , " + video.second+" sec");
                }
                System.out.println("Type remove index");
                int target = scanner.nextInt();
                if(0 < target && target < videoLinkURL.size())
                    videoLinkURL.remove(target-1);
            }
            else{
                return 4;
            }

        }
    }
}
