// TempChargeNum.cpp : 实现文件
//

#include "stdafx.h"
#include "EpGateMaint.h"
#include "TempChargeNum.h"


// CTempChargeNum 对话框

IMPLEMENT_DYNAMIC(CTempChargeNum, CDialog)

CTempChargeNum::CTempChargeNum(CWnd* pParent /*=NULL*/)
	: CDialog(CTempChargeNum::IDD, pParent)
{
    num="";
	time="";
    flag="";

}

CTempChargeNum::~CTempChargeNum()
{
}

void CTempChargeNum::DoDataExchange(CDataExchange* pDX)
{
	CDialog::DoDataExchange(pDX);
}


BEGIN_MESSAGE_MAP(CTempChargeNum, CDialog)
	ON_BN_CLICKED(IDOK, &CTempChargeNum::OnBnClickedOk)
END_MESSAGE_MAP()


// CTempChargeNum 消息处理程序

void CTempChargeNum::OnBnClickedOk()
{
	// TODO: 在此添加控件通知处理程序代码
	OnOK();
	((CEdit *)GetDlgItem(IDC_EDIT1))->GetWindowText(num);//
	((CEdit *)GetDlgItem(IDC_EDIT2))->GetWindowText(time);//
	((CEdit *)GetDlgItem(IDC_EDIT3))->GetWindowText(flag);//
}
