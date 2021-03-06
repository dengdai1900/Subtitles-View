package org.fordes.subview.controller;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Font;
import org.apache.commons.lang3.StringUtils;
import org.fordes.subview.main.Launcher;
import org.fordes.subview.util.FileIOUtil.SubFileUtil;
import org.fordes.subview.util.FindReplaceUtil;
import org.fordes.subview.util.SubtitlesUtil.editUtil;
import org.fordes.subview.util.ToastUtil;


import java.net.URL;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ToolPanelControl implements Initializable {
    @FXML
    private GridPane ToolPanel,FindPanel,ReplacePanel,JumpPanel,CodePanel,StylePanel;
    @FXML
    private ChoiceBox code_box,Style_FontList,Style_FontSizeList;
    @FXML
    private ToggleButton Style_Bold,Style_Italic,Style_Underline;
    @FXML
    private CheckMenuItem find_filter,find_Case,find_Regular,find_all,
                          search_filter,search_Case,search_Regular,search_all;
    @FXML
    private TextField find_input,Replace_search_input,Replace_replace_input,Jump_input;
    @FXML
    private Menu find_option_Menu;
    @FXML
    private Button Jump_btn,code_btn;
    private TextArea editor;
    private startControl controller= (startControl) Launcher.controllers.get(startControl.class.getSimpleName());
    private String previous=null;//上一个关键字
    private FindReplaceUtil fr;//搜索对象
    public String isCall="";
    private String code=null;//记录编码

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        //初始化
        code_box.getItems().addAll("UTF-8", "ANSI（GBK）","Unicode","ASCII");
        Style_FontList.getItems().addAll(new Font(0).getFamilies());
        Style_FontSizeList.getItems().addAll("8","9","10","11","12","13","14","15","16","17","18","19","20","22","24","26","28","36","48","72");
        //默认项
        String temp=controller.inputset.getCode().equals("GBK")?"ANSI（GBK）":controller.inputset.getCode();
        code_box.getSelectionModel().select(temp);
        //绑定查找和替换部分菜单项
        search_filter.selectedProperty().bindBidirectional(find_filter.selectedProperty());
        search_Case.selectedProperty().bindBidirectional(find_Case.selectedProperty());
        search_Regular.selectedProperty().bindBidirectional(find_Regular.selectedProperty());
        search_all.selectedProperty().bindBidirectional(find_all.selectedProperty());
    }

    private Font getFont(){
        //Font f=Font.font("Timer New Roman", FontWeight.BOLD,FontPosture.ITALIC,25);
        return editor.getFont();
    }


    //隐藏所有组件
    private void hideComponent(){
        FindPanel.setVisible(false);
        ReplacePanel.setVisible(false);
        JumpPanel.setVisible(false);
        CodePanel.setVisible(false);
        StylePanel.setVisible(false);
    }

    //功能调用
    public void Call(TextArea TargetEditor,String type){
        this.editor=TargetEditor;//设置目标编辑器
        editor.selectRange(0,0);//功能切换，清空选中
        find_input.setText("");//功能切换清空输入框
        hideComponent();
        isCall=type;
        switch (type){
            case "find":
                find_input.requestFocus();
                //输入框回车以搜索
                find_input.addEventFilter(KeyEvent.KEY_PRESSED, new EventHandler<KeyEvent>() {
                public void handle(KeyEvent ke) {
                    if (ke.getCode() == KeyCode.ENTER) {
                        onFind(null);
                        ke.consume(); // 释放
                    }
                }
                });
                FindPanel.setVisible(true);
                break;
            case "replace":
                Replace_search_input.requestFocus();
                //搜索框回车以搜索
                Replace_search_input.addEventFilter(KeyEvent.KEY_PRESSED, new EventHandler<KeyEvent>() {
                    public void handle(KeyEvent ke) {
                        if (ke.getCode() == KeyCode.ENTER) {
                            onReplace_search(null);
                            ke.consume(); // 释放
                        }
                    }
                });
                //输入框回车以替换
                Replace_replace_input.addEventFilter(KeyEvent.KEY_PRESSED, new EventHandler<KeyEvent>() {
                    public void handle(KeyEvent ke) {
                        if (ke.getCode() == KeyCode.ENTER) {
                            Replace_next(null);
                            ke.consume(); // 释放
                        }
                    }
                });
                ReplacePanel.setVisible(true);
                break;
            case "Jump":
                Jump_input.setText("");
                Jump_input.requestFocus();
                Jump_input.addEventFilter(KeyEvent.KEY_PRESSED, new EventHandler<KeyEvent>() {//输入框回车以搜索
                    public void handle(KeyEvent ke) {
                        if (ke.getCode() == KeyCode.ENTER) {
                            onJumpLine(null);
                            ke.consume(); // 释放
                        }
                    }
                });
                JumpPanel.setVisible(true);
                break;
            case "code":
                //获取当前项目
                code_box.getSelectionModel().select(controller.inputset.getCode().equals("GBK")?"ANSI（GBK）":controller.inputset.getCode());
                code=controller.inputset.getCode();
                //监听编码选项改变
                code_box.getSelectionModel().selectedItemProperty().addListener(new ChangeListener() {
                    @Override
                    public void changed(ObservableValue observable, Object oldValue, Object newValue) {
                        String code=newValue.toString().equals("ANSI（GBK）")?"GBK":newValue.toString();
                    }
                });
                CodePanel.setVisible(true);
                break;
            case "style":
                //读取当前字体
                Style_FontList.getSelectionModel().select(StringUtils.substringBeforeLast(editor.getFont().getName(), " "));
                Style_FontSizeList.getSelectionModel().select(Integer.toString((int)editor.getFont().getSize()));
                StylePanel.setVisible(true);
                //监听字体列表改变
                Style_FontList.getSelectionModel().selectedItemProperty().addListener(new ChangeListener() {
                    @Override
                    public void changed(ObservableValue observable, Object oldValue, Object newValue) {
                        String fontsize=Style_FontSizeList.getSelectionModel().getSelectedItem().toString();
                        editor.setFont(new Font(newValue.toString(),Integer.parseInt(fontsize)));
                    }
                });
                //监听字体大小列表改变
                Style_FontSizeList.getSelectionModel().selectedItemProperty().addListener(new ChangeListener() {
                    @Override
                    public void changed(ObservableValue observable, Object oldValue, Object newValue) {
                        String font=Style_FontList.getSelectionModel().getSelectedItem().toString();
                        editor.setFont(new Font(font,(double)Integer.valueOf(newValue.toString())));
                    }
                });
                break;
        }
        ToolPanel.setVisible(true);
    }

    //关闭事件
    public void close(ActionEvent actionEvent) {
        ToolPanel.setVisible(false);
        find_input.setText("");
        Replace_search_input.setText("");
        Replace_replace_input.setText("");
    }

    //默认样式
    public void Style_default(ActionEvent actionEvent) {
        if(!Style_FontList.getSelectionModel().getSelectedItem().toString().equals("System")||!Style_FontSizeList.getSelectionModel().getSelectedItem().toString().equals("18")) {
            Style_FontList.getSelectionModel().select("System");
            Style_FontSizeList.getSelectionModel().select("18");
            editor.setFont(Font.font("System",(double) 18.0));
            editor.requestFocus();
            new ToastUtil().toast(controller.inputset.getStage(),"已恢复默认样式",controller.inputset.getTheme(),1000);
            return;
        }
    }

    //关键字搜索
    public void onFind(ActionEvent actionEvent) {
        String target=find_input.getText();
            if(target.equals("")||target==null)
                return;
        //构建搜索对象并搜索
        fr=new FindReplaceUtil(editor,target,find_Case.isSelected(),find_Regular.isSelected(),find_filter.isSelected());
        if(!fr.search()) {
            new ToastUtil().toast(controller.inputset.getStage(), "未找到", controller.inputset.getTheme(), 1000);
            editor.selectRange(0,0);
            find_input.requestFocus();
            find_input.selectAll();
        }
    }

    //查找替换面板搜索按钮
    public void onReplace_search(ActionEvent actionEvent) {
        find_input.setText(Replace_search_input.getText());
        onFind(null);
        //Replace_search_input.requestFocus();
    }

    //替换下一个
    public void Replace_next(ActionEvent actionEvent) {
        String target=Replace_search_input.getText();
        String ReplaceTarget=Replace_replace_input.getText();
        /*输入不全无法进行替换*/
        if(target.equals("")||target==null||ReplaceTarget.equals("")||ReplaceTarget==null)
            return;
        //构建搜索对象并搜索
        fr=new FindReplaceUtil(editor,target,find_Case.isSelected(),find_Regular.isSelected(),find_filter.isSelected());
        if(!fr.replace(ReplaceTarget)) {
            new ToastUtil().toast(controller.inputset.getStage(), "未找到", controller.inputset.getTheme(), 1000);
            editor.selectRange(0,0);
            Replace_replace_input.requestFocus();
            Replace_replace_input.selectAll();
        }
    }

    //替换全部
    public void Replace_all(ActionEvent actionEvent) {
        String target=Replace_search_input.getText();
        String ReplaceTarget=Replace_replace_input.getText();
        /*输入不全无法进行替换*/
        if(target.equals("")||target==null||ReplaceTarget.equals("")||ReplaceTarget==null)
            return;
        //构建搜索对象并搜索
        fr=new FindReplaceUtil(editor,target,find_Case.isSelected(),find_Regular.isSelected(),find_filter.isSelected());
        if(!fr.replaceAll(ReplaceTarget)) {
            new ToastUtil().toast(controller.inputset.getStage(), "未找到", controller.inputset.getTheme(), 1000);
            editor.selectRange(0,0);
            Replace_replace_input.requestFocus();
            Replace_replace_input.selectAll();
        }else{
            new ToastUtil().toast(controller.inputset.getStage(), "已替换全部", controller.inputset.getTheme(), 1000);
            editor.selectRange(0,0);
            Replace_replace_input.requestFocus();
            Replace_replace_input.selectAll();
        }
    }

    public void onJumpLine(ActionEvent actionEvent) {
        int pos=0;
        String number=Jump_input.getText().trim();
        Matcher isNum=Pattern.compile("[0-9]*").matcher(number);//正则校验
        if(number.equals("")||number==null||!isNum.matches()||Integer.parseInt(number)==0) {
            Jump_input.setText("");
            new ToastUtil().toast(controller.inputset.getStage(),"输入无效！",controller.inputset.getTheme(),500);
            return;
        }
        int line=Integer.parseInt(number);
        int position=new editUtil().ChangLine(editor,line);
        if(position!=-1){
            editor.positionCaret(position);
            editor.requestFocus();
            close(null);
        }
        else{
            Jump_input.selectAll();
            Jump_input.requestFocus();
            new ToastUtil().toast(controller.inputset.getStage(),"未找到此行",controller.inputset.getTheme(),500);
            return;
        }

    }

   /* 编码改变时刷新文本区*/
    public void Refresh(ActionEvent actionEvent) {
        controller.inputset.setCode(code);
        if(controller.inputset.getSubFile()!=null)
            editor.setText(new SubFileUtil().Read(controller.inputset.getSubFile(),controller.inputset.getCode()));
        new ToastUtil().toast(controller.inputset.getStage(),"已更改编码，如出现问题，请还原至原选项",controller.inputset.getTheme(),1000);

    }
}
