#pragma once
class CEpGateMaintDlg;

// COther 对话框

class COther : public CDialog
{
	DECLARE_DYNAMIC(COther)

public:
	COther(CWnd* pParent = NULL);   // 标准构造函数
	virtual ~COther();

// 对话框数据
	enum { IDD = IDD_OTHER };


CEpGateMaintDlg * m_dlg;

	

protected:
	virtual void DoDataExchange(CDataExchange* pDX);    // DDX/DDV 支持

	DECLARE_MESSAGE_MAP()
public:
	afx_msg void OnBnClickedButton15();
	afx_msg void OnBnClickedButton27();
	afx_msg void OnBnClickedButton26();
	afx_msg void OnBnClickedButton29();
	afx_msg void OnBnClickedButton30();
	afx_msg void OnBnClickedButton31();
	afx_msg void OnBnClickedButton1();
	afx_msg void OnBnClickedButton2();
	afx_msg void OnBnClickedButton3();
	afx_msg void OnBnClickedButton4();
	afx_msg void OnBnClickedButton32();
};
