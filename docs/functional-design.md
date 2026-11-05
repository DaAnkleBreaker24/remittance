> Generated from Remittance_Platform_Functional_Design.docx by scripts/docs-sync.sh. Edit the Word file, then re-run.

**Remittance Platform**

Functional Design: Onboarding, Payments, Wallet, Beneficiaries, Transfers, Reports and Parametrisation

| **Item**       | **Detail**                                                                                                                                                            |
|----------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Scope          | Customer app and back-office for outbound transfers from Mauritius (MUR). First corridor Madagascar (MGA). Further corridors: India, Bangladesh, Nepal, South Africa. |
| Audience       | Product owner, solution designers, development team                                                                                                                   |
| Version / date | Draft 0.2, 5 November 2026 (adds the customer wallet, epic E17)                                                                                                       |
| Status         | For review. Companion file: Remittance_Backlog_Epics_UserStories.xlsx                                                                                                 |

# Contents

1. Introduction

2. Module Overview

3. Customer Onboarding Workflow

4. Profile Management

5. Payment Instruments and Payment Type Selection

6. Beneficiary Workflow with Compliance Check

7. Execute Transfer Workflow

8. Customer Wallet

9. View Transactions and Status

10. Reports Menu

11. Parametrisation Menu (Developer Only)

12. Back-office Menus

13. Cross-cutting Capabilities

14. Non-functional Requirements

15. Epics and Release Plan

16. Open Questions

# 1. Introduction

## 1.1 Purpose

This document defines the functional workflows and menus of the remittance platform from the technology and product side. It is the basis for the epics and user stories in the companion Excel backlog.

## 1.2 Scope

- Customer app: onboarding, profile, payment instruments, beneficiaries, transfers, transaction history.

- Back-office: compliance operations, support, finance and operations, reports.

- Developer-only parametrisation menu for fees, commission, limits and operational settings.

- Corridor model so new countries are added by configuration.

## 1.3 Assumptions and boundaries

- Licensing, regulatory policy and AML/KYC rules are owned by the compliance and legal side. The platform implements those rules as configurable parameters and workflows, and does not define them.

- Cards are collected through a PSP using hosted fields and tokens. The platform never stores card numbers.

- Payout goes through one or more payout partners (aggregator) behind a standard adapter interface.

- MauCAS is a pay-in option supported in two modes behind one connector: DIRECT participation and via a SPONSOR_BANK. The mode is a parameter, so the participation decision can be taken later without redesign (section 5.4).

- Tax on fees is optional. A parameter switches it on or off (default off). The ledger always carries the tax accounts and posting template, so enabling tax later needs no ledger change (section 7.2).

- All amounts are stored as integers in the minor unit of the currency. Fee and limit values in this document are parameters, not fixed numbers.

## 1.4 Actors

| **Actor**                    | **Description**                                                                               |
|------------------------------|-----------------------------------------------------------------------------------------------|
| Customer                     | Person in Mauritius who sends money abroad.                                                   |
| Beneficiary                  | Recipient in the destination country. Not a platform user.                                    |
| Compliance officer           | Reviews onboarding, beneficiary and transaction alerts; makes decisions.                      |
| Support agent                | Helps customers; performs controlled manual actions.                                          |
| Finance / operations analyst | Reconciliation, liquidity, revenue and partner management.                                    |
| Developer / admin            | Maintains parameters, flags and corridors through the parametrisation menu.                   |
| Payout partner / PSP / bank  | External systems: card acquirer, bank, MauCAS, payout aggregator, eKYC and screening vendors. |

# 2. Module Overview

| **Module**            | **Customer menu**                                     | **Back-office / admin**                                   | **Epic** |
|-----------------------|-------------------------------------------------------|-----------------------------------------------------------|----------|
| Onboarding & KYC      | Sign up, verify identity, status                      | Review queue, document view                               | E01      |
| Authentication        | Login, biometric, devices                             | Lockout/recovery tools                                    | E02      |
| Profile               | Details, limits, preferences                          | Customer 360                                              | E03      |
| Payment instruments   | Cards, bank accounts, MauCAS                          | Instrument review                                         | E04      |
| Wallet                | Balance, top up, pay from wallet, withdraw, statement | Wallet 360, freeze, adjustments (four-eyes), safeguarding | E17      |
| Beneficiaries         | Add, edit, status                                     | Screening cases                                           | E05      |
| Quote & pricing       | Amount, fee, rate                                     | Pricing parameters                                        | E06, E11 |
| Transfers             | Send, confirm, receipt                                | Hold, release, manual actions                             | E07      |
| Transactions          | History, detail, status                               | Search, stuck items                                       | E08      |
| Compliance operations | \-                                                    | Case queue, rescreening, audit                            | E09      |
| Reports               | Statements                                            | Business, finance, risk, tax reports                      | E10      |
| Parametrisation       | \-                                                    | Developer-only settings                                   | E11      |
| Finance back-end      | \-                                                    | Ledger, reconciliation, liquidity                         | E12      |
| Notifications         | SMS, push, email                                      | Templates                                                 | E13      |
| Support & refunds     | Help, contact                                         | Console, refunds, disputes                                | E14      |
| Corridors & partners  | Country and channel choice                            | Partner adapters, router                                  | E15      |

# 3. Customer Onboarding Workflow

## 3.1 Flow

| **\#** | **Step**         | **Actor**           | **System behaviour**                                                         | **Rule / output**                                                  |
|--------|------------------|---------------------|------------------------------------------------------------------------------|--------------------------------------------------------------------|
| 1      | Register         | Customer            | Enter mobile number; send OTP                                                | OTP expiry and attempts from parameters; duplicate number rejected |
| 2      | Consent          | Customer            | Show current T&C and privacy notice                                          | Version, timestamp, device and IP stored                           |
| 3      | Personal details | Customer            | Collect name, DOB, nationality, address, occupation, source of funds         | Draft saved; required fields by configuration                      |
| 4      | Identity proof   | Customer            | Capture ID document and selfie with liveness via eKYC vendor                 | Accepted documents depend on nationality; quality checks           |
| 5      | Automated checks | System              | Run onboarding checks (section 3.3)                                          | Each check returns PASS, REVIEW or FAIL                            |
| 6      | Risk scoring     | System              | Compute risk score; assign KYC level                                         | Level determines limits; score factors stored                      |
| 7      | Decision         | System / compliance | All PASS: auto-approve. Any REVIEW: create compliance case. Any FAIL: reject | Four-eyes for defined cases; reasons coded                         |
| 8      | Activation       | System              | Create customer record, set limits, enable sending                           | Notification sent; audit entry written                             |
| 9      | Set up security  | Customer            | Create PIN, enable biometrics, bind device                                   | Required before first transfer                                     |

## 3.2 Onboarding statuses

| **Status**       | **Meaning**                         | **Customer sees**                                    |
|------------------|-------------------------------------|------------------------------------------------------|
| DRAFT            | Started, not submitted              | Continue application                                 |
| SUBMITTED        | Submitted, automated checks running | We are checking your details                         |
| IN_REVIEW        | Compliance case open                | Under review                                         |
| MORE_INFO_NEEDED | Compliance requested documents      | Action required, upload now                          |
| APPROVED         | Active                              | Account ready                                        |
| REJECTED         | Not accepted                        | Safe message and next steps (no tipping-off wording) |
| SUSPENDED        | Blocked after activation            | Contact support                                      |

## 3.3 Onboarding checks

| **Check**                          | **Purpose**                                          | **Outcome handling**                            |
|------------------------------------|------------------------------------------------------|-------------------------------------------------|
| Age and nationality eligibility    | Customer is 18+ and nationality/document is accepted | FAIL if not eligible                            |
| Document authenticity and expiry   | Detect forged, expired or tampered documents         | FAIL or REVIEW per vendor score                 |
| Face match and liveness            | Selfie matches document photo; person is present     | REVIEW on low confidence; retry allowed         |
| Duplicate identity                 | Same person or document already registered           | REVIEW or FAIL                                  |
| Sanctions and PEP screening        | Customer not on lists                                | Hit creates a compliance case; no auto-approval |
| Device and phone risk              | Risky device, SIM swap or reused device              | REVIEW per rules                                |
| Source of funds / occupation rules | Information complete and consistent                  | REVIEW when inconsistent                        |

## 3.4 KYC levels

| **Level** | **Requirements**                                                     | **Limits**                                                |
|-----------|----------------------------------------------------------------------|-----------------------------------------------------------|
| NONE      | Phone verified only; cannot send                                     | No transfers                                              |
| BASIC     | ID document, selfie, personal details, all checks passed             | Per-transaction, daily and monthly limits from parameters |
| FULL      | BASIC plus proof of address and source of funds, compliance approved | Higher limits from parameters                             |

> *Limit values and level criteria are supplied by compliance and stored in the parametrisation menu (section 11).*

# 4. Profile Management

| **Section**          | **Customer can**                                  | **Control**                                                                         |
|----------------------|---------------------------------------------------|-------------------------------------------------------------------------------------|
| Personal details     | View name, DOB, nationality (read-only after KYC) | Changes need re-verification through support                                        |
| Contact details      | Edit email and address                            | OTP/email verification; change history                                              |
| Phone number         | Change registered number                          | Step-up authentication, compliance flag, temporary restriction on new beneficiaries |
| Security             | Change PIN, biometrics, view and remove devices   | Notification on every change                                                        |
| KYC level and limits | View level and limit usage; start upgrade         | Upgrade creates a compliance case                                                   |
| Preferences          | Language, optional notifications                  | Mandatory transactional messages cannot be turned off                               |
| Legal                | View accepted T&C versions                        | Re-acceptance flow when a new version is published                                  |
| Data rights          | Request data export or account closure            | Closure blocked while transactions are open                                         |

# 5. Payment Instruments and Payment Type Selection

## 5.1 Payment types

| **Type**     | **How added**                                                                    | **What is stored**                           | **Pay-in behaviour**                                                    | **Key controls**                                                       |
|--------------|----------------------------------------------------------------------------------|----------------------------------------------|-------------------------------------------------------------------------|------------------------------------------------------------------------|
| Card         | PSP-hosted fields, 3-D Secure                                                    | Token, brand, last 4, expiry                 | Authorise and capture at transfer; funds settled by acquirer            | No card numbers held; name check; BIN country and velocity rules       |
| Bank account | Enter account; verify ownership (method per bank/partner)                        | Bank, masked account, verification status    | Bank transfer or direct debit, confirmed by callback or statement match | Account name must match customer; verification status                  |
| MauCAS       | Link via bank or MauCAS authorisation, or pay per transfer by QR/instant payment | Reference only                               | Payment request tied to transaction reference; confirmation by callback | Works in DIRECT and SPONSOR_BANK modes (section 5.4); timeout handling |
| Wallet       | Opened automatically after onboarding approval (section 8)                       | Balance held in the ledger; no external data | Available balance debited instantly at confirmation                     | Available-balance check; wallet status; no overdraft                   |

## 5.2 Add instrument workflow

| **\#** | **Step**               | **Behaviour**                                                                                              |
|--------|------------------------|------------------------------------------------------------------------------------------------------------|
| 1      | Choose type            | Customer opens Payment methods and selects Card, Bank account or MauCAS                                    |
| 2      | Step-up authentication | Required before adding                                                                                     |
| 3      | Capture                | Card: hosted fields with 3-D Secure verification. Bank: account details. MauCAS: redirect to authorisation |
| 4      | Ownership check        | Holder name compared with customer name; mismatch rejected or sent to review                               |
| 5      | Verification           | Status becomes VERIFIED or REJECTED; reason shown                                                          |
| 6      | Save                   | Customer may set as default; audit entry written                                                           |

## 5.3 Select payment type during transfer

- Available types depend on corridor, KYC level, amount and parameters.

- Each option shows its fee impact and expected confirmation time.

- Customer picks a saved instrument or pays once (for example MauCAS QR) without saving.

- Wallet appears when it is enabled and has an available balance. If the balance is too low, the customer can top up the difference or choose another method.

- Third-party payers are not allowed unless compliance enables an exception.

## 5.4 MauCAS integration modes

MauCAS is built once behind a connector interface (createPaymentRequest, getStatus, callback handling, settlement report). Two implementations exist and the active one is chosen by parameter per environment. The customer experience is identical in both modes.

| **Aspect**      | **DIRECT mode**                                                        | **SPONSOR_BANK mode**                                                     |
|-----------------|------------------------------------------------------------------------|---------------------------------------------------------------------------|
| Connection      | Platform connects to MauCAS as a participant                           | Platform connects to a sponsor bank that connects to MauCAS               |
| Payment request | Created and tracked by the platform connector                          | Created through the bank's API or host-to-host interface                  |
| Confirmation    | Callback or status query from MauCAS                                   | Callback or status query from the bank                                    |
| Settlement      | Settlement position held by the platform; mode-specific ledger account | Settlement through the sponsor bank account; mode-specific ledger account |
| Reconciliation  | Settlement reports vs ledger                                           | Bank statements vs ledger                                                 |
| Switch          | Parameter MAUCAS_MODE = DIRECT                                         | Parameter MAUCAS_MODE = SPONSOR_BANK                                      |

> *A separate parameter enables or disables MauCAS as a payment type, so it can be turned off in any mode. Switching mode applies to new payments; payments already in progress finish in the mode they started with.*

# 6. Beneficiary Workflow with Compliance Check

## 6.1 Flow

| **\#** | **Step**                    | **Actor**          | **System behaviour**                                              | **Rule / output**                                       |
|--------|-----------------------------|--------------------|-------------------------------------------------------------------|---------------------------------------------------------|
| 1      | Select country and channel  | Customer           | Show channels from corridor catalogue (wallet, bank, cash pickup) | Only enabled corridors shown                            |
| 2      | Enter details               | Customer           | Dynamic form from corridor field definitions                      | Format validation (wallet number, IFSC, account format) |
| 3      | Relationship and purpose    | Customer           | Select from configured lists                                      | Mandatory where rule requires                           |
| 4      | Confirm                     | Customer           | Step-up authentication                                            | Beneficiary saved as PENDING_SCREENING                  |
| 5      | Name verification           | System             | Partner lookup where supported                                    | Mismatch warning; result stored                         |
| 6      | Sanctions and PEP screening | System             | Screen name and identifiers                                       | Clear: ACTIVE. Hit: PENDING_REVIEW                      |
| 7      | Risk rules                  | System             | Check shared accounts, country risk, new-beneficiary rules        | May lower limits or require review                      |
| 8      | Compliance decision         | Compliance officer | Approve or reject case                                            | ACTIVE or BLOCKED with reason                           |
| 9      | Notify                      | System             | Inform customer of result                                         | Safe wording only                                       |

## 6.2 Beneficiary statuses

| **Status**        | **Can receive transfers?** | **Notes**                                  |
|-------------------|----------------------------|--------------------------------------------|
| DRAFT             | No                         | Form not finished                          |
| PENDING_SCREENING | No                         | Automatic checks running                   |
| PENDING_REVIEW    | No                         | Compliance case open                       |
| ACTIVE            | Yes                        | Subject to cooling-off limit if configured |
| BLOCKED           | No                         | Rejected or added to blocklist             |
| DELETED           | No                         | Soft deleted; history kept                 |

> *Editing sensitive fields (name, account, wallet) puts the beneficiary back into PENDING_SCREENING. Beneficiaries and customers are rescreened automatically when lists are updated.*

# 7. Execute Transfer Workflow

| **\#** | **Step**                 | **Actor**          | **System behaviour**                                                                                | **Transaction state**                |
|--------|--------------------------|--------------------|-----------------------------------------------------------------------------------------------------|--------------------------------------|
| 1      | Start quote              | Customer           | Choose beneficiary, enter send or receive amount                                                    | \-                                   |
| 2      | Quote                    | System             | Compute fee, commission, FX margin and rate; pre-check limits; lock rate                            | QUOTED                               |
| 3      | Choose payment type      | Customer           | Select instrument; fee impact displayed                                                             | \-                                   |
| 4      | Review and confirm       | Customer           | Summary; accept terms; step-up authentication                                                       | PAYIN_PENDING                        |
| 5      | Collect funds            | System             | Initiate card, bank or MauCAS collection using idempotency key; a wallet payment is debited at once | PAYIN_PENDING                        |
| 6      | Pay-in confirmed         | System             | Verify callback; post ledger entries atomically with state change                                   | PAYIN_CONFIRMED                      |
| 7      | Compliance check         | System             | Sanctions, velocity, limits, country and amount rules                                               | COMPLIANCE_CHECK                     |
| 8      | Manual review (if hold)  | Compliance officer | Approve, reject or request information                                                              | MANUAL_REVIEW                        |
| 9      | Payout                   | System             | Router selects partner; send payout with idempotent reference                                       | PAYOUT_SENT                          |
| 10     | Result                   | System             | Success: complete. Failure: retry or refund. Unknown: query partner first                           | PAID / PAYOUT_RETRY / REFUND_PENDING |
| 11     | Receipt and notification | System             | Send receipt and status messages                                                                    | PAID                                 |
| 12     | Refund (if needed)       | System             | Refund to the original instrument or to the wallet, as the refund policy allows; ledger reversal    | REFUNDED                             |

## 7.1 Failure handling

| **Situation**                          | **Handling**                                                                    |
|----------------------------------------|---------------------------------------------------------------------------------|
| Quote expires before confirmation      | Transaction not created; customer refreshes quote                               |
| Pay-in declined or times out           | Transaction FAILED; customer offered another payment method                     |
| Wallet balance too low at confirmation | Transfer not created; customer tops up the difference or picks another method   |
| Compliance hold                        | Customer sees Processing; compliance decides within SLA; reject leads to refund |
| Payout rejected by partner             | Retry or alternate partner if safe; otherwise refund                            |
| Payout result unknown (timeout)        | Never resend blindly. Query partner by reference, then decide                   |
| Duplicate callback                     | Ignored through webhook inbox deduplication                                     |
| Partner down or low prefunding         | Circuit breaker pauses corridor or channel; customer informed before paying     |

## 7.2 Fee, commission and tax treatment

All fee, commission, FX margin, tax and limit values are parameters; none is fixed in code. The quote shows the breakdown to the customer and stores the same amounts on the transaction.

| **Element**        | **Treatment**                                                                                                                                                                                      |
|--------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Fee and commission | Calculated from the fee tables by corridor, channel, payment type and tier                                                                                                                         |
| FX margin          | Applied to the customer rate within configured guardrails                                                                                                                                          |
| Tax (optional)     | Switch TAX_ENABLED (default off). When on, tax is calculated on the configured base (fee, commission, FX margin) using the tax code and rate, inclusive or exclusive, with the configured rounding |
| Quote and receipt  | Tax line shown only when tax is enabled; amounts on quote, receipt and statement always agree                                                                                                      |
| Ledger             | Tax accounts and posting template exist whether tax is enabled or not. The tax line is zero (or skipped) when disabled. Changing the switch affects new transactions only                          |
| Refund             | Fee and tax refund follows the refund policy parameter; the ledger reverses the same lines                                                                                                         |

**Pay-in posting for one transfer (A = total paid by customer, P = amount to pay out, F = fee, T = tax; A = P + F + T):**

| **Line** | **Account**                                                                | **Direction** | **Amount** | **When tax is disabled**              |
|----------|----------------------------------------------------------------------------|---------------|------------|---------------------------------------|
| 1        | COLLECTION (by payment type), or WALLET_CUSTOMER when paid from the wallet | Debit         | A          | A = P + F                             |
| 2        | PAYOUT_IN_TRANSIT                                                          | Credit        | P          | Same                                  |
| 3        | FEE_REVENUE                                                                | Credit        | F          | Same                                  |
| 4        | TAX_PAYABLE                                                                | Credit        | T          | T = 0, line skipped or posted as zero |

> *TAX_PAYABLE can be reconciled and reported at any time, and shows zero movement while tax is disabled.*

# 8. Customer Wallet

Each customer can hold one wallet in Mauritian rupees. Money is added from a card, bank account or MauCAS. It can then pay for transfers or be withdrawn to the customer's own bank account. The WALLET_ENABLED parameter switches the wallet on, globally or per customer segment.

> *Holding customer money in a wallet may need a separate authorisation and a safeguarding arrangement. Licensing and safeguarding rules are owned by compliance and legal. The platform implements them as parameters, controls and reports (open questions 13 to 16).*

## 8.1 Wallet operations

| **Operation**    | **Customer flow**                                                                                                                                                     | **Rules**                                                                                                         |
|------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------|
| Open wallet      | Created automatically when onboarding is approved and the wallet is enabled; customer accepts the wallet terms once                                                   | One MUR wallet per customer; the KYC level sets wallet limits                                                     |
| Top up           | Choose card, bank account or MauCAS; enter amount; fee shown; step-up authentication; confirm                                                                         | Minimum and maximum top-up, daily and monthly limits, maximum balance. Credited only when the pay-in is confirmed |
| Pay a transfer   | Choose Wallet as the payment type at step 3 of the transfer; available balance shown                                                                                  | Debited from the available balance in the same step that creates the transfer; pay-in confirmed instantly         |
| Withdraw to bank | Choose a verified bank account in the customer's own name; enter amount; fee shown; step-up authentication; confirm                                                   | Withdrawal limits; holding period for card-funded money; paid by bank transfer or MauCAS; tracked until paid      |
| Refund to wallet | Failed or rejected transfers paid from the wallet return to the wallet. For other payment types the customer may choose wallet or original method where policy allows | Refund policy parameter                                                                                           |
| Statement        | Movement list with filters; downloadable monthly statement                                                                                                            | Opening and closing balances tie to the ledger                                                                    |

## 8.2 Balances

- Posted balance: the sum of all posted wallet movements in the ledger.

- Held amount: money reserved for operations in progress, such as a withdrawal not yet paid or card-funded money still in its holding period.

- Available balance = posted balance minus held amount. Only the available balance can be spent or withdrawn.

- The balance can never go below zero. There is no overdraft, credit or interest.

## 8.3 Wallet and operation statuses

| **Object** | **Status**                                                           | **Meaning**                                                                             |
|------------|----------------------------------------------------------------------|-----------------------------------------------------------------------------------------|
| Wallet     | ACTIVE                                                               | Normal use                                                                              |
| Wallet     | FROZEN                                                               | Set by compliance or support with approval; no debits; credits only if policy allows    |
| Wallet     | DORMANT                                                              | No customer activity for the dormancy period; reactivation needs step-up authentication |
| Wallet     | CLOSED                                                               | Remaining balance paid to the customer's bank account first; no further use             |
| Top-up     | PENDING, then COMPLETED or FAILED                                    | Wallet credited only on COMPLETED                                                       |
| Withdrawal | REQUESTED, IN_REVIEW (if rules hit), SENT, PAID; or FAILED, RETURNED | Amount held at REQUESTED; released back to the available balance on FAILED or RETURNED  |

## 8.4 Ledger postings

Each wallet is a liability sub-account (WALLET_CUSTOMER) in the ledger, because the money belongs to the customer. F is the fee and T the tax (zero when tax is disabled).

| **Event**                                                                   | **Debit**                                                                         | **Credit**                                               |
|-----------------------------------------------------------------------------|-----------------------------------------------------------------------------------|----------------------------------------------------------|
| Top-up confirmed (customer pays A; wallet receives W; A = W + F + T)        | COLLECTION (by payment type): A                                                   | WALLET_CUSTOMER: W; FEE_REVENUE: F; TAX_PAYABLE: T       |
| Transfer paid from the wallet (A = P + F + T)                               | WALLET_CUSTOMER: A                                                                | PAYOUT_IN_TRANSIT: P; FEE_REVENUE: F; TAX_PAYABLE: T     |
| Withdrawal requested (wallet debited W; customer receives N; W = N + F + T) | WALLET_CUSTOMER: W                                                                | WITHDRAWAL_IN_TRANSIT: N; FEE_REVENUE: F; TAX_PAYABLE: T |
| Withdrawal paid to the bank                                                 | WITHDRAWAL_IN_TRANSIT: N                                                          | SETTLEMENT_BANK: N                                       |
| Withdrawal failed or returned                                               | Reversal of the withdrawal postings                                               | Reversal of the withdrawal postings                      |
| Refund of a transfer to the wallet                                          | PAYOUT_IN_TRANSIT: P (plus FEE_REVENUE and TAX_PAYABLE as the refund policy says) | WALLET_CUSTOMER                                          |

> *Daily safeguarding reconciliation: the total of all wallet balances is compared with the balance of the safeguarding bank account. Any shortfall raises a critical alert.*

## 8.5 Controls

- Withdrawals go only to a verified bank account in the customer's own name. A newly added withdrawal account has a cooling-off period.

- Card-funded money is held for a period set by parameter before it can be withdrawn, to limit chargeback losses.

- Monitoring rules flag top-ups withdrawn without being used for transfers, rapid top-up and withdrawal cycles, and top-ups from many different cards.

- Freeze, unfreeze and manual adjustments are back-office actions with four-eyes approval. Adjustments are new ledger entries, never edits.

- Every wallet operation carries an idempotency key, and the wallet is locked during each debit so the same money cannot be spent twice.

# 9. View Transactions and Status

## 9.1 Customer-facing statuses

| **Customer status** | **Internal states**                              | **Message**                           |
|---------------------|--------------------------------------------------|---------------------------------------|
| Awaiting payment    | QUOTED, PAYIN_PENDING                            | We are waiting for your payment       |
| Processing          | PAYIN_CONFIRMED, COMPLIANCE_CHECK, MANUAL_REVIEW | We are processing your transfer       |
| Sent                | PAYOUT_SENT, PAYOUT_RETRY                        | Your money is on its way              |
| Delivered           | PAID                                             | Your recipient has received the money |
| Failed              | FAILED, EXPIRED                                  | Transfer not completed                |
| Refund in progress  | REFUND_PENDING                                   | We are returning your money           |
| Refunded            | REFUNDED                                         | Your money has been returned          |

## 9.2 Screens

- List: newest first, filters by date, status and beneficiary, pagination.

- Detail: amounts, fee, rate, payment method, reference, timeline of steps with timestamps, receipt, send again, help.

- Real-time updates through push and in-app refresh; estimated delivery shown while in progress.

# 10. Reports Menu

| **Report**                    | **Audience**            | **Key content and filters**                                                                                    | **Frequency**    |
|-------------------------------|-------------------------|----------------------------------------------------------------------------------------------------------------|------------------|
| Transactions                  | Operations, finance     | Count and value by date, corridor, channel, payment type, status                                               | Daily, on demand |
| Revenue                       | Finance, management     | Fees, commission, FX margin by corridor and period; ties to ledger                                             | Daily, monthly   |
| Tax                           | Finance                 | Tax collected by period, code and corridor; ties to TAX_PAYABLE; zero when tax is disabled                     | Monthly          |
| Daily reconciliation          | Finance                 | Pay-in vs ledger vs partner statements; matched, unmatched, ageing                                             | Daily            |
| Liquidity and prefunding      | Treasury, operations    | Balance per partner and currency, thresholds, days of cover                                                    | Intraday         |
| Wallet balances and movements | Finance, compliance     | Balances by customer and segment, top-ups, withdrawals, transfers paid from the wallet, holds, dormant wallets | Daily, monthly   |
| Safeguarding reconciliation   | Finance                 | Total wallet balances vs safeguarding bank account; differences and ageing                                     | Daily            |
| Failed, stuck and pending     | Support, operations     | Items by age and reason, with action links                                                                     | Intraday         |
| Onboarding funnel             | Product, compliance     | Sign-up to approval to first transfer, drop-off by step, KYC pass rate                                         | Weekly           |
| Compliance                    | Compliance              | Alerts, cases, SLA, decisions, rejections, rescreening hits                                                    | Daily, monthly   |
| Partner performance           | Operations, procurement | Success rate, delivery time, cost, incidents per partner                                                       | Weekly           |
| Limits and high value         | Compliance, risk        | Limit utilisation, high-value and unusual transfers                                                            | Daily            |
| Refunds and disputes          | Support, finance        | Refunds, chargebacks, complaints, ageing                                                                       | Weekly           |
| Customer base                 | Product                 | Active customers, new vs repeat, top beneficiaries by corridor                                                 | Monthly          |
| Regulatory extracts           | Compliance              | Data extracts in formats defined by compliance                                                                 | As required      |

> *All reports support export to CSV/XLSX, role-based access and masked personal data by default.*

# 11. Parametrisation Menu (Developer Only)

## 11.1 Access and control

- Visible only to the developer/admin role with MFA and IP allow-list; every access is logged.

- Maker-checker: a second authorised person approves each change before it takes effect.

- Each parameter has a type, valid range, scope, effective date and version. Changes can be scheduled and rolled back.

- Scope precedence: customer segment \> payment type \> channel \> corridor \> global.

- Before approval, a simulator shows the effect, for example the resulting fee for sample amounts.

## 11.2 Parameter catalogue

| **Group**      | **Parameter**                                                                                       | **Scope**                             | **Type**         |
|----------------|-----------------------------------------------------------------------------------------------------|---------------------------------------|------------------|
| Pricing        | Transfer fee (flat, percentage, tiered, minimum, maximum)                                           | Corridor, channel, payment type, tier | Table            |
| Pricing        | Commission (platform and partner)                                                                   | Corridor, partner                     | Table            |
| Pricing        | FX margin and guardrails                                                                            | Corridor, amount band                 | Percentage       |
| Pricing        | Promo and fee waiver rules                                                                          | Campaign                              | Rule             |
| Tax            | TAX_ENABLED switch (default off)                                                                    | Global, corridor                      | Boolean          |
| Tax            | Tax code, rate, base (fee, commission, FX margin), inclusive or exclusive, rounding, effective date | Corridor, tax code                    | Table            |
| Limits         | Per-transaction, daily and monthly limits                                                           | KYC level, corridor                   | Amount           |
| Limits         | New-beneficiary cooling-off period and cap                                                          | Global, corridor                      | Duration, amount |
| Risk           | Velocity thresholds and review triggers                                                             | Global, corridor                      | Rule             |
| Risk           | Third-party payer exception                                                                         | Global                                | Boolean          |
| Quote          | Quote expiry; rate staleness threshold                                                              | Global, corridor                      | Duration         |
| Authentication | OTP validity and attempts; lockout; session timeout                                                 | Global                                | Number, duration |
| Operations     | Retry counts and delays; payout cut-off times                                                       | Partner, corridor                     | Number, time     |
| Operations     | Corridor, channel and partner on/off switches                                                       | Corridor, channel, partner            | Boolean          |
| Operations     | MAUCAS_ENABLED and MAUCAS_MODE (DIRECT / SPONSOR_BANK)                                              | Environment                           | Boolean, enum    |
| Liquidity      | Low-balance thresholds and auto-pause                                                               | Partner, currency                     | Amount, boolean  |
| Wallet         | WALLET_ENABLED switch (default off)                                                                 | Global, customer segment              | Boolean          |
| Wallet         | Maximum balance; minimum and maximum top-up; daily and monthly top-up and withdrawal limits         | KYC level                             | Amount           |
| Wallet         | Top-up and withdrawal fees (flat, percentage, minimum, maximum)                                     | Payment type, channel                 | Table            |
| Wallet         | Holding period for card-funded balance; cooling-off for new withdrawal accounts                     | Global                                | Duration         |
| Wallet         | Dormancy period; refund-to-wallet policy                                                            | Global                                | Duration, enum   |
| Compliance     | Accepted documents, required fields, KYC criteria                                                   | Nationality, level                    | List             |
| Content        | T&C version, status mapping, notification templates                                                 | Language                              | Text             |

> *Every fee, commission, margin, tax and limit value is a parameter. Values are seeded per environment at deployment and changed only through the menu with approval.*

# 12. Back-office Menus

| **Menu**               | **Functions**                                                                                                                                                | **Roles**                    |
|------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------|
| Compliance cases       | Queue with SLA, assignment, decisions with reasons, four-eyes, request info, rescreening, blocklist/allowlist, suspicious activity case pack                 | Compliance officer           |
| Customer 360           | Profile, KYC documents, instruments, beneficiaries, transactions, notes, audit                                                                               | Compliance, support (masked) |
| Support console        | Search, view timeline, retry, refund, mark resolved with approval, tickets and disputes                                                                      | Support agent                |
| Finance                | Reconciliation breaks, prefunding, partner top-ups, revenue, chargebacks                                                                                     | Finance analyst              |
| Wallet                 | Wallet 360 (balance, holds, movements), freeze and unfreeze, manual adjustment with four-eyes approval, withdrawal review queue, safeguarding reconciliation | Support, finance, compliance |
| Partners and corridors | Partner health, routing priority, corridor and channel status                                                                                                | Operations, developer        |
| Administration         | Users, roles, audit log, parametrisation (developer only)                                                                                                    | Admin                        |

# 13. Cross-cutting Capabilities

These capabilities span several modules. Each is covered by an epic in the backlog.

| **Area**                              | **Why it matters**                                | **Handling**                                                                                                                 | **Epic**      |
|---------------------------------------|---------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------|---------------|
| Authentication and device binding     | Account takeover is the main fraud risk           | PIN, biometrics, device binding, step-up                                                                                     | E02           |
| Rate lock and quote expiry            | Customers must see the exact price                | Locked quote with expiry and refresh                                                                                         | E06           |
| Limits enforcement and display        | Prevents surprises and risk                       | Pre-check at quote; usage on profile                                                                                         | E03, E06      |
| Ledger and reconciliation             | Money must always balance                         | Double-entry ledger, daily reconciliation                                                                                    | E12           |
| Prefunding and liquidity              | Payouts fail when partner balance is low          | Balance tracking, alerts, auto-pause                                                                                         | E12           |
| Refunds, cancellation and chargebacks | Failures and mistakes will happen                 | Automatic refund, cancel before payout, chargeback workflow                                                                  | E07, E14      |
| Receipts and proof of payment         | Customers and beneficiaries ask for proof         | PDF/in-app receipts and share link                                                                                           | E07, E08      |
| Notifications                         | Customers need status without calling support     | Event-driven SMS/push/email in several languages                                                                             | E13           |
| Support console and four-eyes actions | Safe manual intervention                          | Role-based actions with approval                                                                                             | E14           |
| Compliance rescreening and audit      | Lists change after onboarding                     | Scheduled rescreening, immutable audit                                                                                       | E05, E09      |
| Partner failover and monitoring       | Single partner is a single point of failure       | Router, circuit breakers, health metrics                                                                                     | E15, E16      |
| Tax on fees                           | Tax may apply to fees in some cases               | On/off parameter (default off); tax calculated from parameters; ledger tax accounts and posting always in place              | E06, E11, E12 |
| MauCAS modes                          | Participation model is decided later              | Connector with DIRECT and SPONSOR_BANK modes selected by parameter; mode-specific settlement and reconciliation              | E04, E12      |
| Customer wallet                       | Faster repeat transfers; money kept for later use | MUR stored value with top-up, pay from wallet and withdrawal; liability sub-ledger; holds; daily safeguarding reconciliation | E17, E12      |
| Data protection and retention         | Personal and ID data is sensitive                 | Encryption, retention rules, export and deletion                                                                             | E03, E16      |
| Terms versioning and consent          | Proof of acceptance is needed                     | Versioned T&C with re-acceptance flow                                                                                        | E01           |
| Localisation                          | Customers speak several languages                 | Template-based multi-language content                                                                                        | E13           |
| Cash pickup and agent network         | Common in some corridors                          | Out of scope in R1; confirm per corridor                                                                                     | E15           |
| Environments and testing              | Partner failures must be testable                 | Mock partner, sandbox, load and penetration tests                                                                            | E15, E16      |

# 14. Non-functional Requirements

| **Area**             | **Requirement**                                                                                                                            |
|----------------------|--------------------------------------------------------------------------------------------------------------------------------------------|
| Availability         | Customer API target 99.9%; payout delivery target defined per corridor                                                                     |
| Integrity            | Ledger always balanced; all state changes atomic with ledger and events                                                                    |
| Idempotency          | Every external call, webhook and transaction creation is idempotent                                                                        |
| Security             | Encryption in transit and at rest, field-level encryption for PII, secrets manager, RBAC, immutable audit, penetration test before go-live |
| Performance          | Quote and transfer-create paths load-tested to agreed targets                                                                              |
| Observability        | Transaction ID in logs, metrics and traces; business alerts for failures, stuck items and low liquidity                                    |
| Recovery             | Point-in-time recovery, tested restores and defined RPO/RTO                                                                                |
| Accessibility and UX | Clear status language, readable on low-end phones, supports several languages                                                              |

# 15. Epics and Release Plan

| **Epic** | **Name**                                 | **Stories** | **Points** |
|----------|------------------------------------------|-------------|------------|
| E01      | Onboarding & KYC                         | 11          | 48         |
| E02      | Authentication & Account Security        | 7           | 25         |
| E03      | Profile Management                       | 7           | 22         |
| E04      | Payment Instruments (Card, Bank, MauCAS) | 10          | 63         |
| E05      | Beneficiaries & Compliance Screening     | 10          | 39         |
| E06      | Quote, Fees & FX                         | 10          | 43         |
| E07      | Transfer Execution                       | 14          | 77         |
| E08      | Transaction History & Tracking           | 7           | 18         |
| E09      | Compliance Operations (Back-office)      | 8           | 39         |
| E10      | Reports & Dashboards                     | 14          | 56         |
| E11      | Parametrisation (Developer Admin)        | 13          | 51         |
| E12      | Ledger, Reconciliation & Liquidity       | 11          | 64         |
| E13      | Notifications                            | 5           | 18         |
| E14      | Support, Refunds & Disputes              | 7           | 30         |
| E15      | Corridors & Payout Partners              | 11          | 76         |
| E16      | Platform, Security & Observability       | 12          | 64         |
| E17      | Customer Wallet (Stored Value)           | 19          | 86         |

| **Release**   | **Scope**                                                                                                                                                                                                                               |
|---------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| R1: MVP       | Mauritius to Madagascar, mobile money payout, card, bank and MauCAS pay-in (both MauCAS modes), optional tax parameter with ledger support, full onboarding, compliance queue, core reports, parametrisation, ledger and reconciliation |
| R2: Hardening | Second partner and failover, bank payouts, name verification, cancellation, extra reports, KYC upgrade and refresh; customer wallet (top-up, pay from wallet, withdraw to own bank account)                                             |
| R3: Expansion | India, Bangladesh, Nepal, South Africa; promo codes, scheduled transfers, statements                                                                                                                                                    |

> *Story points are first estimates and should be re-estimated with the development team. The Excel file holds all stories with acceptance criteria, priority, release, status, owner and sprint.*

# 16. Open Questions

| **\#** | **Question**                                                                                                                                                           | **Related epic** |
|--------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------|
| 1      | Which aggregator covers each corridor and channel, and what are the settlement currencies?                                                                             | E15              |
| 2      | Which MauCAS mode goes live (direct or sponsor bank)? Both are built; decision deferred and set by parameter                                                           | E04              |
| 3      | Which acquirer/PSP will handle cards?                                                                                                                                  | E04              |
| 4      | Which eKYC vendor, and which documents per nationality?                                                                                                                | E01              |
| 5      | KYC levels and limits to be seeded                                                                                                                                     | E11              |
| 6      | Screening lists and vendor                                                                                                                                             | E05              |
| 7      | If tax is enabled: rate, base and inclusive/exclusive treatment (decision deferred, default off)                                                                       | E06              |
| 8      | Data residency and retention rules                                                                                                                                     | E16              |
| 9      | Regulatory report formats and deadlines                                                                                                                                | E10              |
| 10     | Refund policy details                                                                                                                                                  | E14              |
| 11     | Cash pickup in scope for any corridor?                                                                                                                                 | E15              |
| 12     | Languages required at launch                                                                                                                                           | E13              |
| 13     | Does holding customer money in a wallet need a separate licence or authorisation, and what are the safeguarding rules (trust account, bank, reconciliation frequency)? | E17              |
| 14     | Wallet limits per KYC level (maximum balance, top-up, withdrawal) and top-up and withdrawal fees                                                                       | E17              |
| 15     | Are card top-ups allowed? If yes, how long is card-funded money held before it can be withdrawn?                                                                       | E17              |
| 16     | Wallet is planned for R2. Confirm, or bring it forward to R1                                                                                                           | E17              |
