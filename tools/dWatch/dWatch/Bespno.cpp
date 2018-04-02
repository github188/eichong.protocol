// Bespno.cpp : 实现文件
//

#include "stdafx.h"
#include "EpGateMaint.h"
#include "Bespno.h"


// CBespno 对话框

IMPLEMENT_DYNAMIC(CBespno, CDialog)

CBespno::CBespno(CWnd* pParent /*=NULL*/)
	: CDialog(CBespno::IDD, pParent)
{
     redo="0";
	flag=1;
	timet="0";
	bespno="0";
	
}

CBespno::~CBespno()
{
}

void CBespno::DoDataExchange(CDataExchange* pDX)
{
	CDialog::DoDataExchange(pDX);
	DDX_Control(pDX, IDC_COMBO1, m_payMode);
	DDX_Control(pDX, IDC_COMBO2, m_BespnoTime);
}

BOOL CBespno::OnInitDialog()
{
	CDialog::OnInitDialog();

	m_payMode.AddString("先付费");
    m_payMode.AddString("后付费");
	m_payMode.SetCurSel(0);

	for(int i=1;i<13;i++)
	{
		CString str;
		str.Format("%d",i*30);
	    m_BespnoTime.AddString(str);
	}
	m_BespnoTime.SetCurSel(0);
    m_payMode.SetCurSel(0);
	OnBnClickedButton1();

	((CButton *)GetDlgItem(IDC_RADIO1))->SetCheck(TRUE);
	return true;
}

BEGIN_MESSAGE_MAP(CBespno, CDialog)
	ON_BN_CLICKED(IDC_RADIO1, &CBespno::OnBnClickedRadio1)
	ON_BN_CLICKED(IDC_RADIO2, &CBespno::OnBnClickedRadio2)
	ON_BN_CLICKED(IDOK, &CBespno::OnBnClickedOk)
	ON_BN_CLICKED(IDC_BUTTON1, &CBespno::OnBnClickedButton1)
END_MESSAGE_MAP()


// CBespno 消息处理程序

void CBespno::OnBnClickedRadio1()
{
	// TODO: 在此添加控件通知处理程序代码
	  redo="0";
	  ((CButton *)GetDlgItem(IDC_RADIO1))->SetCheck(TRUE);
	  ((CButton *)GetDlgItem(IDC_RADIO2))->SetCheck(FALSE);
	  
}

void CBespno::OnBnClickedRadio2()
{
	// TODO: 在此添加控件通知处理程序代码
	  redo="1";
	  ((CButton *)GetDlgItem(IDC_RADIO1))->SetCheck(FALSE);
	  ((CButton *)GetDlgItem(IDC_RADIO2))->SetCheck(TRUE);
}

void CBespno::OnBnClickedOk()
{
	// TODO: 在此添加控件通知处理程序代码
	OnOK();
	int i=m_BespnoTime.GetCurSel();
	 timet.Format("%d",(i+1)*30);
	
	 ((CEdit *)GetDlgItem(IDC_EDIT9))->GetWindowText(bespno);

	if(bespno=="")
		bespno="0";

	 i= m_payMode.GetCurSel();
	payType.Format("%d",i+1);
	((CEdit *)GetDlgItem(IDC_EDIT10))->GetWindowText(pw);//
}

void CBespno::OnBnClickedButton1()
{
	// TODO: 在此添加控件通知处理程序代码
	   time_t t=time(NULL);
	   srand((unsigned) t);
	   int k = rand() % (99 - 10 + 1) +10;

	   CString bespnoT,sk;
	   bespnoT.Format("%d",t);
	   sk.Format("%d",k);

     ((CEdit *)GetDlgItem(IDC_EDIT9))->SetWindowText(bespnoT+sk);//
	 
}
