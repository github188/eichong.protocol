// TestEpGate.cpp : 实现文件
//

#include "stdafx.h"
#include "EpGateMaint.h"
#include "TestEpGate.h"
#include "EpGateMaintDlg.h"




// CTestEpGate 对话框

IMPLEMENT_DYNAMIC(CTestEpGate, CDialog)

CTestEpGate::CTestEpGate(CWnd* pParent /*=NULL*/)
	: CDialog(CTestEpGate::IDD, pParent)
{
    m_dlg = (CEpGateMaintDlg *)pParent;
}

CTestEpGate::~CTestEpGate()
{
}

void CTestEpGate::DoDataExchange(CDataExchange* pDX)
{
	CDialog::DoDataExchange(pDX);
}


BEGIN_MESSAGE_MAP(CTestEpGate, CDialog)
	ON_BN_CLICKED(IDC_BUTTON6, &CTestEpGate::OnBnClickedButton6)
	ON_BN_CLICKED(IDC_BUTTON7, &CTestEpGate::OnBnClickedButton7)
	ON_BN_CLICKED(IDC_BUTTON8, &CTestEpGate::OnBnClickedButton8)
	ON_BN_CLICKED(IDC_BUTTON9, &CTestEpGate::OnBnClickedButton9)
	ON_BN_CLICKED(IDC_BUTTON10, &CTestEpGate::OnBnClickedButton10)
	ON_BN_CLICKED(IDC_BUTTON11, &CTestEpGate::OnBnClickedButton11)
	ON_BN_CLICKED(IDC_BUTTON12, &CTestEpGate::OnBnClickedButton12)
	ON_BN_CLICKED(IDC_BUTTON1, &CTestEpGate::OnBnClickedButton1)
	ON_BN_CLICKED(IDC_BUTTON2, &CTestEpGate::OnBnClickedButton2)
END_MESSAGE_MAP()


// CTestEpGate 消息处理程序

void CTestEpGate::OnBnClickedButton6()
{
	// TODO: 在此添加控件通知处理程序代码
	m_dlg->OnBnClickedButton_startCharge();
}

void CTestEpGate::OnBnClickedButton7()
{
	// TODO: 在此添加控件通知处理程序代码
	m_dlg->OnBnClickedButton_endCharge();
}

void CTestEpGate::OnBnClickedButton8()
{
	// TODO: 在此添加控件通知处理程序代码
	m_dlg->OnBnClickedButton_startBesp();
}

void CTestEpGate::OnBnClickedButton9()
{
	// TODO: 在此添加控件通知处理程序代码
	m_dlg->OnBnClickedButton_endBesp();
}

void CTestEpGate::OnBnClickedButton10()
{
	// TODO: 在此添加控件通知处理程序代码
	m_dlg->OnBnClickedButton_callEp();
}

void CTestEpGate::OnBnClickedButton11()
{
	// TODO: 在此添加控件通知处理程序代码
	m_dlg->OnBnClickedButton_dropClock();
}

void CTestEpGate::OnBnClickedButton12()
{
	// TODO: 在此添加控件通知处理程序代码
	m_dlg->OnBnClickedButton_sendRate();
}

void CTestEpGate::OnBnClickedButton1()
{
	// TODO: 在此添加控件通知处理程序代码
	m_dlg->OnBnClickedButton_createIdentyCode();
}

void CTestEpGate::OnBnClickedButton2()
{
	// TODO: 在此添加控件通知处理程序代码
	m_dlg->OnBnClickedButton_stationSetEpCode();
}
