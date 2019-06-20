package download;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class Main extends Application{

	private static int threadNum = 4;
	public static ProgressBar[] progressBars =new ProgressBar[threadNum];
			
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		Application.launch(args);
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		// TODO Auto-generated method stub
		Scene scene = new Scene(new Group(),300,200);
		primaryStage.setTitle("Muti-Thread DownloadTools");
		primaryStage.setScene(scene);
		
		VBox vBox = new VBox();
		vBox.setSpacing(10);
		vBox.setPadding(new Insets(10,10,10,10));
		TextField urlText = new TextField();
		urlText.setPrefSize(280, 20);
		vBox.getChildren().add(urlText);
		//int threadNum =4;
		for(int i=0;i<threadNum;i++) {
			progressBars[i] = new ProgressBar(0);
			progressBars[i].setPrefSize(250, 20);
			vBox.getChildren().add(progressBars[i]);
		}
		HBox hBox = new HBox();
		hBox.setSpacing(10);
		Button startbutton = new Button("开始");
		startbutton.setPrefSize(100, 30);
		Button stopbutton = new Button("暂停");
		stopbutton.setPrefSize(100, 30);
		hBox.getChildren().addAll(startbutton,stopbutton);
		vBox.getChildren().add(hBox);
		((Group)scene.getRoot()).getChildren().addAll(vBox);
		
		String filepath = urlText.getText();
		//String filepath = "http://172.17.0.111/cache/11/01/www.ipmsg.org.cn/f0bcc5b65133d2903f87aed844041636/Feige_for_windows.exe";
        DownloadThread task = new DownloadThread(filepath ,threadNum);
		
		startbutton.setOnAction(new EventHandler<ActionEvent>() {
			
			@Override
			public void handle(ActionEvent event) {
				// TODO Auto-generated method stub
				task.setFilepath(urlText.getText());
		        task.downloadPart();
			}
		});
		
		stopbutton.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent event) {
				// TODO Auto-generated method stub
				task.setFilepath(urlText.getText());
				task.stopThread();
			}
		});
		
		primaryStage.show();
	}

}
