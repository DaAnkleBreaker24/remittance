# Backlog (generated from Remittance_Backlog_Epics_UserStories.xlsx by scripts/docs-sync.sh)

The Excel workbook is the source of truth for status and owners. This file is a read-only copy so Claude Code can see the stories.

Work the stories in the coding order given in CLAUDE.md.


## E01 Onboarding & KYC

### E01-US01 Register with mobile number and OTP  (Must, R1, 3 pts)
As a customer, I want to sign up with my mobile number and verify it by OTP so that I can start onboarding securely.
Acceptance:
  - OTP expires after configured time
  - max attempts then temporary lock
  - duplicate numbers rejected

### E01-US02 Accept terms and privacy consent  (Must, R1, 2 pts)
As a customer, I want to read and accept the current terms and privacy notice so that my consent is recorded.
Acceptance:
  - Versioned T&C stored with timestamp, device and IP
  - cannot continue without acceptance

### E01-US03 Capture personal details  (Must, R1, 3 pts)
As a customer, I want to enter my name, date of birth, nationality, address, occupation and source of funds so that the platform can verify who I am.
Acceptance:
  - Field validation
  - draft saved and resumable
  - required fields driven by configuration

### E01-US04 Upload ID document and selfie (eKYC)  (Must, R1, 8 pts)
As a customer, I want to scan my passport, national ID or work permit and take a selfie with liveness so that my identity is verified remotely.
Acceptance:
  - Accepted document types configurable per nationality
  - image quality feedback
  - vendor result stored
  - manual fallback path
Note: Depends on eKYC vendor selection

### E01-US05 Run automated onboarding checks  (Must, R1, 8 pts)
As the platform, I need to run sanctions/PEP screening, document authenticity, duplicate-identity and age checks so that risky applicants are caught before activation.
Acceptance:
  - Result PASS / REVIEW / FAIL per check
  - REVIEW routed to compliance queue
  - all results stored and auditable

### E01-US06 Risk-score customer and assign KYC level  (Must, R1, 5 pts)
As the platform, I need to compute a risk score and assign a KYC level with limits so that limits match the customer risk.
Acceptance:
  - Scoring rules configurable
  - factors recorded
  - level drives limits in E11

### E01-US07 Show onboarding status to customer  (Must, R1, 3 pts)
As a customer, I want to see where my application stands so that I know what to expect.
Acceptance:
  - Statuses: Draft, Submitted, In review, More info needed, Approved, Rejected
  - notification on every change

### E01-US08 Request additional information  (Should, R1, 3 pts)
As a compliance officer, I want to ask a customer for more documents or explanations so that I can resolve review cases.
Acceptance:
  - Request text and due date
  - customer uploads in app
  - case returns to queue on submission

### E01-US09 Upgrade KYC level  (Should, R2, 5 pts)
As a customer, I want to submit extra documents such as proof of address and source of funds so that I can send higher amounts.
Acceptance:
  - Triggered when limit reached or voluntarily
  - compliance approval required
  - limits update on approval

### E01-US10 Handle rejection and re-application  (Should, R2, 3 pts)
As a customer, I want to receive a clear outcome and know if I can re-apply so that I understand next steps.
Acceptance:
  - Reason codes mapped to safe customer wording
  - cooldown configurable
  - no tipping-off wording

### E01-US11 Periodic KYC refresh and document expiry  (Should, R2, 5 pts)
As the platform, I need to prompt customers when documents expire or refresh is due so that KYC stays valid.
Acceptance:
  - Expiry tracked
  - reminders
  - sending blocked after grace period if configured


## E02 Authentication & Account Security

### E02-US01 Login with PIN/password and OTP  (Must, R1, 3 pts)
As a customer, I want to log in securely so that only I can access my account.
Acceptance:
  - Credentials hashed
  - OTP on new session per policy
  - generic error messages

### E02-US02 Biometric login  (Should, R1, 3 pts)
As a customer, I want to use Face ID or fingerprint so that login is fast.
Acceptance:
  - Opt-in
  - fallback to PIN
  - disabled after biometric change per OS rules

### E02-US03 Device binding and new-device verification  (Must, R1, 5 pts)
As a customer, I want to register my device and verify any new one so that stolen credentials are not enough.
Acceptance:
  - Device ID bound after OTP
  - new device triggers verification and notification
  - max devices configurable

### E02-US04 Step-up authentication for sensitive actions  (Must, R1, 5 pts)
As a customer, I want to confirm sensitive actions with an extra factor so that fraud is harder.
Acceptance:
  - Required for add beneficiary, send, change phone, change PIN
  - method configurable

### E02-US05 Session management and remote logout  (Should, R1, 3 pts)
As a customer, I want to be logged out when idle and end other sessions so that my account stays safe.
Acceptance:
  - Idle timeout configurable
  - token revocation
  - session list

### E02-US06 Account lockout and recovery  (Must, R1, 3 pts)
As a customer, I want to recover access if I forget my PIN or lose my phone so that I am not locked out forever.
Acceptance:
  - Lock after failed attempts
  - recovery needs identity re-verification
  - all events audited

### E02-US07 Root/jailbreak detection and certificate pinning  (Should, R2, 3 pts)
As the platform, I need to harden the mobile app so that the client is harder to attack.
Acceptance:
  - Policy configurable
  - blocked or restricted when device compromised


## E03 Profile Management

### E03-US01 View profile, KYC level and limits  (Must, R1, 2 pts)
As a customer, I want to see my details, KYC level and limits so that I know what I can do.
Acceptance:
  - Read-only KYC fields
  - limits shown from parameters

### E03-US02 Edit contact details with re-verification  (Must, R1, 3 pts)
As a customer, I want to update my email and address so that my information is current.
Acceptance:
  - OTP/email verification on change
  - change history stored

### E03-US03 Change phone number  (Should, R1, 5 pts)
As a customer, I want to change my registered mobile number so that I keep access after changing SIM.
Acceptance:
  - Step-up auth
  - compliance flag
  - 24h restriction on new beneficiaries/high value (configurable)

### E03-US04 Change PIN/password and security settings  (Must, R1, 2 pts)
As a customer, I want to manage my credentials and biometric setting so that I control my security.
Acceptance:
  - Old credential check
  - notification sent

### E03-US05 Set language and notification preferences  (Should, R1, 2 pts)
As a customer, I want to choose language and notification channels so that I get messages the way I want.
Acceptance:
  - Mandatory transactional messages cannot be disabled

### E03-US06 Show limit usage  (Must, R1, 3 pts)
As a customer, I want to see how much of my limit I have used so that I can plan transfers.
Acceptance:
  - Daily/monthly usage and reset date
  - updates after each transfer

### E03-US07 Request data export or account closure  (Should, R2, 5 pts)
As a customer, I want to ask for my data or to close my account so that my data rights are respected.
Acceptance:
  - Closure blocked if open transactions
  - retention rules applied
  - request logged


## E04 Payment Instruments (Card, Bank, MauCAS)

### E04-US01 Select payment type at transfer  (Must, R1, 3 pts)
As a customer, I want to choose card, bank transfer or MauCAS so that I pay the way I prefer.
Acceptance:
  - Available types depend on corridor, KYC level and parameters
  - fee impact shown

### E04-US02 Save card via PSP-hosted fields  (Must, R1, 8 pts)
As a customer, I want to save my card securely so that I can pay faster next time.
Acceptance:
  - No card numbers stored by us
  - token, brand, last 4 and expiry only
  - 3-D Secure supported
Note: Depends on PSP/acquirer contract

### E04-US03 Add and verify bank account  (Must, R1, 8 pts)
As a customer, I want to add my bank account for pay-in so that I can pay by bank transfer.
Acceptance:
  - Account name must match customer name
  - verification method per bank/partner
  - status Pending / Verified / Rejected

### E04-US04 MauCAS connector with selectable mode (direct or via sponsor bank)  (Must, R1, 13 pts)
As the platform, I need to offer MauCAS through one connector interface with two implementations, DIRECT and SPONSOR_BANK so that the participation decision can be made later without redesign.
Acceptance:
  - Mode set by parameter per environment
  - same interface for createPaymentRequest, getStatus, callback
  - switch needs no code change
  - each mode has own settlement accounts and reconciliation
  - MauCAS can be enabled or disabled independently
Note: Participation model undecided: build both modes

### E04-US05 Link MauCAS instrument  (Should, R1, 8 pts)
As a customer, I want to link or pay through MauCAS so that I can pay with instant local payment.
Acceptance:
  - Authorisation flow depends on active mode
  - store reference only
  - behaviour identical for the customer in both modes

### E04-US06 Pay with MauCAS instant payment/QR without saving  (Should, R2, 8 pts)
As a customer, I want to pay a single transfer using MauCAS so that I do not need to save an instrument.
Acceptance:
  - Payment request created with transaction reference
  - confirmation via callback
  - timeout handled
  - works in both MauCAS modes
Note: Participation model undecided: build both modes

### E04-US07 List, set default and remove instruments  (Must, R1, 3 pts)
As a customer, I want to manage my saved instruments so that I stay in control.
Acceptance:
  - Removal blocked if linked to open transaction
  - audit of changes

### E04-US08 Ownership and third-party payer check  (Must, R1, 5 pts)
As the platform, I need to ensure the payer is the customer so that third-party funding is controlled.
Acceptance:
  - Name match
  - mismatch routed to review or rejected per rule

### E04-US09 Card fraud controls  (Should, R1, 5 pts)
As the platform, I need to apply 3-D Secure, velocity and BIN-country checks so that card fraud losses are reduced.
Acceptance:
  - Rules configurable
  - declined reasons logged
  - alerts to compliance/risk

### E04-US10 Handle expired or declined instruments  (Should, R2, 2 pts)
As a customer, I want to be told clearly when a payment method fails so that I can fix it quickly.
Acceptance:
  - Error mapped to friendly message
  - option to switch method


## E05 Beneficiaries & Compliance Screening

### E05-US01 Add beneficiary with corridor-driven form  (Must, R1, 5 pts)
As a customer, I want to add a recipient using the fields required for their country and channel so that payouts reach the right person.
Acceptance:
  - Form generated from corridor field definitions
  - step-up auth
  - draft saved

### E05-US02 Validate beneficiary fields  (Must, R1, 3 pts)
As the platform, I need to validate wallet numbers, IFSC, account formats and names so that bad data is caught early.
Acceptance:
  - Format rules per corridor/channel
  - clear error messages

### E05-US03 Verify beneficiary name with partner  (Should, R2, 5 pts)
As a customer, I want to have the recipient account checked before sending so that I avoid sending to the wrong person.
Acceptance:
  - Partner lookup where supported
  - mismatch warning
  - result stored

### E05-US04 Screen beneficiary against sanctions and PEP lists  (Must, R1, 5 pts)
As the platform, I need to screen every new or changed beneficiary so that sanctioned parties cannot receive funds.
Acceptance:
  - Screening on create/edit
  - hit puts beneficiary in Pending review
  - blocked beneficiary cannot be used

### E05-US05 Beneficiary status lifecycle  (Must, R1, 3 pts)
As a customer, I want to see whether a beneficiary is pending, active or blocked so that I know if I can send.
Acceptance:
  - Statuses: Draft, Pending screening, Pending review, Active, Blocked, Deleted
  - notification on change

### E05-US06 Capture relationship and purpose of transfer  (Must, R1, 2 pts)
As a customer, I want to state my relationship to the beneficiary and usual purpose so that compliance data is complete.
Acceptance:
  - Values from configurable lists
  - mandatory where rule requires

### E05-US07 Edit and delete beneficiary with re-screening  (Must, R1, 3 pts)
As a customer, I want to change or remove a beneficiary so that my list stays accurate.
Acceptance:
  - Sensitive field change triggers re-screening
  - soft delete keeps history

### E05-US08 New beneficiary cooling-off limit  (Should, R2, 3 pts)
As the platform, I need to apply a lower limit to newly added beneficiaries so that fraud exposure is reduced.
Acceptance:
  - Duration and cap configurable
  - message explains limit

### E05-US09 Ongoing rescreening on list updates  (Should, R2, 5 pts)
As the platform, I need to rescreen beneficiaries and customers when lists change so that new sanctions hits are found quickly.
Acceptance:
  - Batch job on list update
  - new hits create compliance cases

### E05-US10 Detect shared or suspicious beneficiaries  (Should, R2, 5 pts)
As the platform, I need to flag the same wallet or account used by many customers so that mule patterns are detected.
Acceptance:
  - Rule thresholds configurable
  - alert to compliance queue


## E06 Quote, Fees & FX

### E06-US01 Request quote by send or receive amount  (Must, R1, 5 pts)
As a customer, I want to enter either amount and see the other so that I know what the recipient gets.
Acceptance:
  - Both directions supported
  - rounding rules per currency

### E06-US02 Show fee, rate and total transparently  (Must, R1, 3 pts)
As a customer, I want to see fee, exchange rate, total to pay and amount received so that there are no surprises.
Acceptance:
  - Breakdown always shown
  - rate shown as customer rate
  - currency formats correct

### E06-US03 Rate lock with expiry and refresh  (Must, R1, 3 pts)
As a customer, I want to have my rate locked for a short time so that the price does not move while I confirm.
Acceptance:
  - Expiry configurable
  - expired quote cannot be used
  - easy refresh

### E06-US04 Fee, commission and margin engine  (Must, R1, 8 pts)
As the platform, I need to calculate fee, commission and FX margin from parameters so that pricing is configurable without code.
Acceptance:
  - Flat, percentage, tiered and min/max supported
  - scope by corridor, channel, payment type, tier
  - calculation stored on quote

### E06-US05 Pre-check limits at quote  (Must, R1, 3 pts)
As the platform, I need to check per-transaction, daily and monthly limits so that customers know early if they exceed limits.
Acceptance:
  - Result shown before payment
  - message suggests KYC upgrade when relevant

### E06-US06 Rate source integration with staleness guard  (Must, R1, 5 pts)
As the platform, I need to fetch rates from provider/partner and reject stale rates so that quotes are accurate.
Acceptance:
  - Fallback source
  - stale threshold configurable
  - corridor paused if no valid rate

### E06-US07 Payment-method-based pricing  (Should, R1, 3 pts)
As the platform, I need to vary fee by payment type so that card costs can be recovered.
Acceptance:
  - Parameter-driven
  - shown in breakdown

### E06-US08 Promo codes and fee waivers  (Could, R3, 5 pts)
As a customer, I want to apply a promo code so that I can get discounts.
Acceptance:
  - Validity, usage caps, corridor scope
  - cost tracked in reports

### E06-US09 Show tax line in quote and receipt when tax is enabled  (Must, R1, 3 pts)
As a customer, I want to see any tax on the fee as a separate line so that the total I pay is transparent.
Acceptance:
  - Tax line shown only when enabled
  - inclusive or exclusive treatment respected
  - same values on quote, receipt and statement

### E06-US10 Tax calculation engine driven by parameters  (Must, R1, 5 pts)
As the platform, I need to calculate tax on fee, commission or FX margin from tax parameters so that tax can be switched on and configured without code.
Acceptance:
  - Global on/off switch
  - tax code, rate, base (fee, commission, margin)
  - inclusive or exclusive
  - rounding rule
  - effective dating
  - per corridor override
  - amount stored on quote and transaction (zero when off)


## E07 Transfer Execution

### E07-US01 Review and confirm transfer  (Must, R1, 3 pts)
As a customer, I want to see a summary before paying so that I can check details.
Acceptance:
  - Shows beneficiary, amounts, fee, rate, payment method, estimated delivery
  - step-up auth to confirm

### E07-US02 Create transaction with idempotency  (Must, R1, 5 pts)
As the platform, I need to create each transaction once even on retries so that duplicates never occur.
Acceptance:
  - Idempotency key enforced
  - repeated call returns same transaction

### E07-US03 Initiate pay-in by payment type  (Must, R1, 8 pts)
As the platform, I need to start collection via card, bank or MauCAS so that funds are collected.
Acceptance:
  - Adapter per payment type
  - pending state tracked
  - timeout handled

### E07-US04 Process pay-in confirmation webhooks  (Must, R1, 5 pts)
As the platform, I need to confirm pay-in from PSP/bank/MauCAS callbacks so that state follows real payment status.
Acceptance:
  - Signature verified
  - deduplicated
  - state transition and ledger posting atomic

### E07-US05 Run transaction compliance checks  (Must, R1, 8 pts)
As the platform, I need to screen sender, beneficiary and transaction against rules so that risky transfers are held.
Acceptance:
  - Sanctions, velocity, limits, country and amount rules
  - outcome PASS / HOLD / REJECT
  - decisions audited

### E07-US06 Dispatch payout through partner  (Must, R1, 8 pts)
As the platform, I need to send the payout to the selected partner so that the beneficiary is paid.
Acceptance:
  - Partner reference stored
  - request idempotent
  - status PAYOUT_SENT

### E07-US07 Handle payout success, failure and unknown  (Must, R1, 8 pts)
As the platform, I need to process partner results correctly so that money is never lost or paid twice.
Acceptance:
  - UNKNOWN triggers status query before retry
  - failure triggers retry or refund
  - all outcomes ledgered

### E07-US08 Automatic refund on failure  (Must, R1, 5 pts)
As the platform, I need to refund the customer if payout cannot complete so that customers are protected.
Acceptance:
  - Refund to original instrument
  - ledger reversal
  - notification
  - refund SLA tracked

### E07-US09 Cancel transfer before payout  (Should, R2, 5 pts)
As a customer, I want to cancel a transfer that has not been paid out so that I can fix mistakes.
Acceptance:
  - Allowed only in defined states
  - refund triggered
  - cut-off enforced

### E07-US10 Generate receipt  (Should, R1, 3 pts)
As a customer, I want to get a receipt for each transfer so that I have proof.
Acceptance:
  - PDF/in-app receipt with reference, rate, fee and status
  - shareable

### E07-US11 Duplicate-transfer protection  (Should, R1, 3 pts)
As a customer, I want to be warned about a similar transfer sent minutes ago so that I do not send twice by mistake.
Acceptance:
  - Rule on beneficiary, amount and time window
  - confirm to proceed

### E07-US12 Payout retry and failover to alternate partner  (Should, R2, 5 pts)
As the platform, I need to retry or switch partner on failure so that delivery rate is higher.
Acceptance:
  - Safe-to-retry rules
  - max attempts configurable
  - partner attempts recorded

### E07-US13 Scheduled and recurring transfers  (Could, R3, 8 pts)
As a customer, I want to schedule a transfer or repeat monthly so that I do not have to remember.
Acceptance:
  - Rate locked at execution
  - reminders
  - cancel anytime

### E07-US14 Refund and cancellation reverse tax and fee as configured  (Should, R1, 3 pts)
As the platform, I need to reverse tax together with the fee on refunds so that ledger and customer amounts stay correct.
Acceptance:
  - Fee and tax refund policy parameter
  - ledger reversal lines
  - receipt of refund shows breakdown


## E08 Transaction History & Tracking

### E08-US01 List transactions with filters  (Must, R1, 3 pts)
As a customer, I want to see my transfers and filter by date, status and beneficiary so that I can find any transfer.
Acceptance:
  - Pagination
  - filters
  - newest first

### E08-US02 Transaction detail with timeline  (Must, R1, 3 pts)
As a customer, I want to open a transfer and see each step so that I can follow progress.
Acceptance:
  - Timeline of states with timestamps
  - amounts, rate, fee, references

### E08-US03 Map internal states to customer statuses  (Must, R1, 2 pts)
As the platform, I need to show simple statuses such as Processing, Sent, Delivered, Failed, Refunded so that customers are not confused by technical states.
Acceptance:
  - Mapping table maintained in configuration

### E08-US04 Real-time status updates  (Should, R1, 3 pts)
As a customer, I want to get updates as my transfer progresses so that I do not need to check manually.
Acceptance:
  - Push and in-app refresh
  - delivery estimate shown

### E08-US05 Send again from history  (Should, R2, 2 pts)
As a customer, I want to repeat a previous transfer so that repeat transfers are quick.
Acceptance:
  - Prefills beneficiary and amount
  - new quote required

### E08-US06 Share proof of payment  (Should, R2, 2 pts)
As a customer, I want to share a confirmation with the recipient so that the recipient can see it was sent.
Acceptance:
  - Link or PDF without sensitive internal data

### E08-US07 Export statements  (Could, R3, 3 pts)
As a customer, I want to download my transaction statement so that I can keep records.
Acceptance:
  - PDF/CSV by period
  - access logged


## E09 Compliance Operations (Back-office)

### E09-US01 Compliance case queue  (Must, R1, 8 pts)
As a compliance officer, I want to see onboarding, beneficiary and transaction alerts in one queue so that I can work cases efficiently.
Acceptance:
  - Priority and SLA timers
  - assignment
  - filters

### E09-US02 Case decision with four-eyes  (Must, R1, 5 pts)
As a compliance officer, I want to approve, reject or request information with reasons so that decisions are consistent and controlled.
Acceptance:
  - Reason codes
  - second approver for defined cases
  - immutable decision record

### E09-US03 Rescreen customer base on list updates  (Must, R1, 5 pts)
As the platform, I need to rescreen customers and beneficiaries after list updates so that new matches are found promptly.
Acceptance:
  - Scheduled and on-demand
  - cases auto-created

### E09-US04 Flag suspicious activity and export case file  (Must, R1, 5 pts)
As a compliance officer, I want to mark activity as suspicious and export a case pack so that I can file reports through the required channel.
Acceptance:
  - Case pack with customer, transactions, notes
  - access restricted
  - filing itself handled by compliance team

### E09-US05 Customer 360 view  (Should, R1, 5 pts)
As a compliance officer, I want to see profile, KYC, instruments, beneficiaries and transactions together so that I can investigate quickly.
Acceptance:
  - Single screen
  - links to documents
  - sensitive data masked by role

### E09-US06 Audit trail of compliance actions  (Must, R1, 3 pts)
As a compliance officer, I want to have every action logged so that reviews and audits are possible.
Acceptance:
  - Who, what, when, before/after values
  - immutable

### E09-US07 Manage blocklist and allowlist  (Should, R2, 3 pts)
As a compliance officer, I want to block or allow customers, beneficiaries, devices or accounts so that known bad actors are stopped.
Acceptance:
  - Entries with reason and expiry
  - applied in real time

### E09-US08 Scenario rules management view  (Should, R2, 5 pts)
As a compliance officer, I want to view and propose AML rule thresholds so that rules reflect risk appetite.
Acceptance:
  - Read view of thresholds
  - changes go through E11 maker-checker


## E10 Reports & Dashboards

### E10-US01 Transaction report  (Must, R1, 3 pts)
As a finance/ops analyst, I want to report transactions by date, corridor, channel and status so that I can monitor business activity.
Acceptance:
  - Filters and totals
  - export CSV/XLSX

### E10-US02 Revenue report  (Must, R1, 5 pts)
As a finance/ops analyst, I want to see fees, FX margin and commission by corridor so that I understand profitability.
Acceptance:
  - Reconciles to ledger revenue accounts

### E10-US03 Daily reconciliation report  (Must, R1, 5 pts)
As a finance/ops analyst, I want to see matched and unmatched items for pay-in, ledger and partners so that breaks are fixed quickly.
Acceptance:
  - Per corridor and partner
  - aging of breaks

### E10-US04 Liquidity and prefunding report  (Must, R1, 3 pts)
As a finance/ops analyst, I want to see prefunded balances per currency and burn rate so that payouts never fail for lack of funds.
Acceptance:
  - Balance, thresholds, projected days of cover

### E10-US05 Failed, stuck and pending transactions report  (Must, R1, 3 pts)
As a support agent, I want to see transactions needing attention so that I can resolve them.
Acceptance:
  - Age buckets
  - reason
  - action links

### E10-US06 Customer and onboarding funnel report  (Should, R1, 5 pts)
As the platform, I need to measure sign-up to first transfer conversion so that product teams can improve it.
Acceptance:
  - Drop-off by step
  - KYC pass rate
  - time to approve

### E10-US07 Compliance report  (Should, R1, 5 pts)
As a compliance officer, I want to see alerts, SLA, decisions and rejections so that I can manage workload and quality.
Acceptance:
  - By analyst, rule and period

### E10-US08 Partner performance report  (Should, R2, 5 pts)
As a finance/ops analyst, I want to compare success rate, speed and cost by partner so that routing and negotiation are data-driven.
Acceptance:
  - By corridor and channel

### E10-US09 Limits and high-value report  (Should, R2, 3 pts)
As a compliance officer, I want to see limit utilisation and high-value transfers so that I can monitor risk.
Acceptance:
  - Thresholds configurable

### E10-US10 Refund and dispute report  (Should, R2, 3 pts)
As a support agent, I want to see refunds, chargebacks and complaints so that I can track loss and service quality.
Acceptance:
  - Reasons, ageing, amounts

### E10-US11 Scheduled delivery and export  (Should, R2, 3 pts)
As a finance/ops analyst, I want to receive reports automatically and export them so that I do not run them manually.
Acceptance:
  - Email schedule
  - CSV/XLSX
  - access controlled

### E10-US12 Executive KPI dashboard  (Could, R2, 5 pts)
As the platform, I need to see volume, revenue, success rate and active customers so that leadership has a quick view.
Acceptance:
  - Daily refresh
  - drill-down

### E10-US13 Regulatory return data extracts  (Should, R2, 5 pts)
As a compliance officer, I want to extract data in the formats compliance specifies so that regulatory reporting is easier.
Acceptance:
  - Formats defined by compliance team
  - extracts reproducible
Note: Formats to come from compliance team

### E10-US14 Tax report  (Should, R1, 3 pts)
As a finance/ops analyst, I want to report tax collected by period, rate and corridor so that tax obligations can be tracked.
Acceptance:
  - Reconciles to TAX_PAYABLE
  - shows zero when tax disabled
  - export


## E11 Parametrisation (Developer Admin)

### E11-US01 Parameter registry with scopes  (Must, R1, 8 pts)
As a developer/admin, I want to manage typed parameters scoped globally, per corridor, channel or tier so that behaviour changes without code.
Acceptance:
  - Types validated
  - scope precedence defined
  - read via cache with TTL

### E11-US02 Fee and commission tables  (Must, R1, 5 pts)
As a developer/admin, I want to define flat, percentage, tiered and min/max fees and commission so that pricing can change safely.
Acceptance:
  - Effective dating
  - simulation tool showing resulting fee

### E11-US03 FX margin configuration  (Must, R1, 3 pts)
As a developer/admin, I want to set FX margin per corridor and amount band so that margin is controlled.
Acceptance:
  - Min/max guardrails
  - change log

### E11-US04 Limits configuration  (Must, R1, 3 pts)
As a developer/admin, I want to set per-transaction, daily and monthly limits per KYC level so that risk limits are enforced.
Acceptance:
  - Validation that limits are consistent
  - effective dating

### E11-US05 Corridor and payout method switches  (Must, R1, 3 pts)
As a developer/admin, I want to enable or pause corridors, channels and partners so that I can react to incidents.
Acceptance:
  - Takes effect without deploy
  - reason required
  - alert sent

### E11-US06 Maker-checker approval  (Must, R1, 5 pts)
As a developer/admin, I want to require a second person to approve parameter changes so that errors and abuse are prevented.
Acceptance:
  - Maker cannot approve own change
  - pending changes visible
  - notification to checker

### E11-US07 Versioning, effective dating and rollback  (Must, R1, 5 pts)
As a developer/admin, I want to schedule changes and roll back so that changes are safe.
Acceptance:
  - Full version history
  - one-click rollback creates new version

### E11-US08 Parameter audit log  (Must, R1, 2 pts)
As a developer/admin, I want to see who changed what and when so that changes are traceable.
Acceptance:
  - Before/after value
  - reason
  - immutable

### E11-US09 Developer-only access controls  (Must, R1, 3 pts)
As a developer/admin, I want to restrict the menu to authorised developers/admins so that sensitive settings are protected.
Acceptance:
  - Role-based
  - MFA
  - IP allow-list
  - access logged

### E11-US10 Operational parameters  (Should, R1, 3 pts)
As a developer/admin, I want to configure quote expiry, OTP validity, retry counts, cooling-off, cut-off times and T&C versions so that operations are tunable.
Acceptance:
  - Catalogue documented
  - validation ranges
  - defaults seeded

### E11-US11 Promote parameters between environments  (Could, R2, 3 pts)
As a developer/admin, I want to export and import parameter sets so that staging and production stay consistent.
Acceptance:
  - Diff view
  - approval required for production import

### E11-US12 Tax configuration  (Must, R1, 5 pts)
As a developer/admin, I want to enable or disable tax and manage tax codes, rates and bases so that tax treatment can change safely.
Acceptance:
  - Switch default OFF
  - maker-checker
  - effective dating and rollback
  - simulation of fee with and without tax

### E11-US13 Value-parametrised fees and limits (no hardcoded values)  (Must, R1, 3 pts)
As a developer/admin, I want to keep every fee, commission, margin and limit value in parameters so that business can adjust values without deployment.
Acceptance:
  - No monetary value in code
  - seed files per environment
  - validation on save


## E12 Ledger, Reconciliation & Liquidity

### E12-US01 Immutable double-entry ledger  (Must, R1, 8 pts)
As the platform, I need to record all money movements in a double-entry ledger so that balances are provable.
Acceptance:
  - Debits equal credits per currency
  - no update/delete
  - idempotent postings

### E12-US02 Ledger postings per transaction event  (Must, R1, 5 pts)
As the platform, I need to post entries for pay-in, fee, payout, refund and reversal so that accounting follows the transaction.
Acceptance:
  - Posting templates configurable
  - atomic with state change

### E12-US03 Reconcile pay-in daily  (Must, R1, 8 pts)
As a finance/ops analyst, I want to match PSP, bank and MauCAS settlements to the ledger so that collected funds are accounted for.
Acceptance:
  - Automated matching
  - breaks flagged
  - per payment type

### E12-US04 Reconcile partner payouts daily  (Must, R1, 8 pts)
As a finance/ops analyst, I want to match partner statements to the ledger so that payouts are accounted for.
Acceptance:
  - Automated matching by reference
  - breaks flagged

### E12-US05 Break management workflow  (Should, R1, 5 pts)
As a finance/ops analyst, I want to investigate and resolve reconciliation breaks so that issues do not accumulate.
Acceptance:
  - Assignment, notes, resolution codes, ageing

### E12-US06 Reconcile MauCAS settlements per mode  (Should, R1, 8 pts)
As a finance/ops analyst, I want to reconcile MauCAS settlements for direct and sponsor-bank modes so that collected funds are accounted for in either mode.
Acceptance:
  - Direct: settlement position vs ledger
  - Sponsor bank: bank statements vs ledger
  - mode-specific ledger accounts
Note: Participation model undecided: build both modes

### E12-US07 Prefunding tracking and low-balance alerts  (Must, R1, 5 pts)
As a finance/ops analyst, I want to track partner balances and get alerts so that payouts do not fail.
Acceptance:
  - Thresholds per currency
  - alert channels
  - payout auto-pause optional

### E12-US08 FX gain/loss and revenue accounting  (Should, R2, 5 pts)
As a finance/ops analyst, I want to report realised FX gain/loss and revenue so that finance close is accurate.
Acceptance:
  - Entries posted per settlement

### E12-US09 Partner funding requests  (Should, R2, 5 pts)
As a finance/ops analyst, I want to raise and track top-ups to partners so that liquidity is managed.
Acceptance:
  - Request, approval, confirmation, ledger posting

### E12-US10 End-of-day ledger integrity check  (Must, R1, 2 pts)
As the platform, I need to verify ledger balance daily so that errors are detected immediately.
Acceptance:
  - Alert on any imbalance
  - report archived

### E12-US11 Tax ledger accounts and postings independent of enablement  (Must, R1, 5 pts)
As the platform, I need to keep tax accounts and posting templates active whether tax is enabled or not so that enabling tax later needs no ledger change.
Acceptance:
  - TAX_PAYABLE account always present
  - posting template always includes tax line, amount zero or skipped when disabled
  - enabling or disabling affects only new transactions
  - refund reverses tax line
  - tax balance reportable and reconcilable


## E13 Notifications

### E13-US01 Multi-language notification templates  (Must, R1, 5 pts)
As the platform, I need to send SMS, push and email using templates so that messages are consistent.
Acceptance:
  - Placeholders
  - languages EN/FR (others later)
  - version control

### E13-US02 Event-driven notifications  (Must, R1, 5 pts)
As the platform, I need to notify on onboarding, beneficiary and transaction events so that customers stay informed.
Acceptance:
  - Events mapped to templates
  - no sensitive data in SMS

### E13-US03 Notification preferences  (Should, R1, 2 pts)
As a customer, I want to choose optional notifications so that I only receive what I want.
Acceptance:
  - Transactional messages always sent

### E13-US04 Template management in admin  (Should, R2, 3 pts)
As a developer/admin, I want to edit templates without deployment so that wording can be improved quickly.
Acceptance:
  - Preview
  - approval
  - version history

### E13-US05 Delivery tracking and fallback  (Should, R2, 3 pts)
As the platform, I need to track delivery and fall back to another channel so that critical messages arrive.
Acceptance:
  - Provider receipts stored
  - fallback rules


## E14 Support, Refunds & Disputes

### E14-US01 Support console search  (Must, R1, 5 pts)
As a support agent, I want to search customers and transactions so that I can help quickly.
Acceptance:
  - Search by phone, name, reference
  - masked sensitive data

### E14-US02 Manual actions with four-eyes  (Must, R1, 5 pts)
As a support agent, I want to retry, refund or resolve stuck transactions with approval so that I can fix issues safely.
Acceptance:
  - Actions limited by state
  - second approval for money movement
  - audit

### E14-US03 Refund rules and execution  (Must, R1, 5 pts)
As the platform, I need to refund to the original instrument under defined rules so that customers are treated consistently.
Acceptance:
  - Rules by payment type
  - timing
  - ledger reversal
  - notification

### E14-US04 In-app help and contact  (Should, R1, 2 pts)
As a customer, I want to find help and contact support so that I can get assistance.
Acceptance:
  - FAQ
  - contact options
  - transaction-specific help

### E14-US05 Ticket and dispute logging  (Should, R2, 5 pts)
As a support agent, I want to log tickets linked to transactions so that issues are tracked.
Acceptance:
  - Categories
  - SLA
  - status
  - link to transaction

### E14-US06 Chargeback handling  (Should, R2, 5 pts)
As a finance/ops analyst, I want to manage card chargebacks so that losses are controlled.
Acceptance:
  - Evidence upload
  - deadlines
  - ledger entries

### E14-US07 Complaint register  (Should, R2, 3 pts)
As a compliance officer, I want to record and track complaints so that regulatory obligations are met.
Acceptance:
  - Fields and SLA defined by compliance


## E15 Corridors & Payout Partners

### E15-US01 Corridor model  (Must, R1, 5 pts)
As the platform, I need to define corridors by send/receive country and currency so that new countries are configuration.
Acceptance:
  - Corridor table drives quotes, limits, fields, channels

### E15-US02 Payout method catalog per corridor  (Must, R1, 3 pts)
As the platform, I need to define channels per corridor such as wallet, bank and cash pickup so that each country uses the right rails.
Acceptance:
  - Channel, currency, limits, cut-offs, required fields

### E15-US03 Partner adapter interface and Madagascar adapter  (Must, R1, 13 pts)
As the platform, I need to integrate the payout partner through a standard adapter so that partners are interchangeable.
Acceptance:
  - sendPayout, getStatus, parseWebhook, getBalance
  - sandbox tests pass
Note: Needs aggregator contract and sandbox

### E15-US04 Per-corridor beneficiary field definitions  (Must, R1, 5 pts)
As the platform, I need to define beneficiary fields per corridor in JSON schema so that forms and validation adapt per country.
Acceptance:
  - Versioned schema
  - used by app and API

### E15-US05 Mock partner and test harness  (Must, R1, 5 pts)
As a developer/admin, I want to simulate delays, timeouts, duplicates and rejections so that failure paths are tested.
Acceptance:
  - Scenario switches
  - used in CI

### E15-US06 Partner health monitoring and circuit breakers  (Should, R1, 5 pts)
As the platform, I need to detect partner degradation and stop sending so that customers do not lose money.
Acceptance:
  - Thresholds configurable
  - auto-pause and alert

### E15-US07 Payout router with failover  (Should, R2, 8 pts)
As the platform, I need to choose partner by rules, cost and success rate so that delivery is reliable and cheap.
Acceptance:
  - Priority list per corridor
  - failover
  - decision logged

### E15-US08 Launch corridor: India  (Should, R3, 8 pts)
As the platform, I need to enable Mauritius to India so that customers can send to India.
Acceptance:
  - Bank rails and beneficiary fields
  - partner tested
  - limits and pricing set

### E15-US09 Launch corridor: Bangladesh  (Should, R3, 8 pts)
As the platform, I need to enable Mauritius to Bangladesh so that customers can send to Bangladesh.
Acceptance:
  - Wallet and bank rails
  - partner tested
  - limits and pricing set

### E15-US10 Launch corridor: Nepal  (Should, R3, 8 pts)
As the platform, I need to enable Mauritius to Nepal so that customers can send to Nepal.
Acceptance:
  - Wallet and bank rails
  - partner tested
  - limits and pricing set

### E15-US11 Launch corridor: South Africa  (Should, R3, 8 pts)
As the platform, I need to enable Mauritius to South Africa so that customers can send to South Africa.
Acceptance:
  - Bank rails
  - partner tested
  - limits and pricing set


## E16 Platform, Security & Observability

### E16-US01 Infrastructure as code and environments  (Must, R1, 8 pts)
As a developer/admin, I want to provision dev, staging and production consistently so that environments are reproducible.
Acceptance:
  - Network, DB, queues, secrets defined as code

### E16-US02 CI/CD with security scanning  (Must, R1, 5 pts)
As a developer/admin, I want to build, test and deploy automatically so that releases are safe and fast.
Acceptance:
  - Unit, integration, SAST, dependency scan gates

### E16-US03 Secrets management and key rotation  (Must, R1, 5 pts)
As a developer/admin, I want to keep secrets out of code and rotate them so that credentials are protected.
Acceptance:
  - Central secrets store
  - rotation process

### E16-US04 PII encryption and data segregation  (Must, R1, 5 pts)
As the platform, I need to encrypt sensitive data and isolate PII so that data breaches have limited impact.
Acceptance:
  - Field-level encryption
  - separate schema
  - masked in logs

### E16-US05 Observability and alerting  (Must, R1, 8 pts)
As a developer/admin, I want to collect logs, metrics and traces by transaction ID so that incidents are detected early.
Acceptance:
  - Business alerts: payout failures, stuck items, low liquidity, ledger imbalance

### E16-US06 Back-office RBAC and audit  (Must, R1, 5 pts)
As the platform, I need to enforce role-based access in all admin tools so that least privilege applies.
Acceptance:
  - Roles defined
  - access reviews
  - immutable audit

### E16-US07 Event bus, outbox and idempotency framework  (Must, R1, 5 pts)
As a developer/admin, I want to publish events reliably and deduplicate so that messages are not lost or duplicated.
Acceptance:
  - Outbox in same DB transaction
  - consumers idempotent

### E16-US08 Backup, point-in-time recovery and DR drills  (Must, R1, 5 pts)
As a developer/admin, I want to back up and test recovery so that data can be restored.
Acceptance:
  - Restore tested
  - RPO/RTO documented

### E16-US09 WAF, rate limiting and bot protection  (Should, R1, 3 pts)
As a developer/admin, I want to protect public endpoints so that abuse is limited.
Acceptance:
  - Rules for onboarding, login and OTP endpoints

### E16-US10 Performance and load testing  (Should, R1, 5 pts)
As a developer/admin, I want to test quote and transfer paths under load so that launch capacity is known.
Acceptance:
  - Targets defined
  - report produced

### E16-US11 Penetration test and remediation  (Must, R1, 5 pts)
As a developer/admin, I want to test security independently so that vulnerabilities are fixed before launch.
Acceptance:
  - Critical/high findings closed before go-live

### E16-US12 Data retention and deletion implementation  (Should, R2, 5 pts)
As the platform, I need to apply retention and deletion rules so that we meet data protection obligations.
Acceptance:
  - Rules supplied by compliance/legal
  - automated jobs
  - logs
Note: Retention periods to come from compliance/legal


## E17 Customer Wallet (Stored Value)

### E17-US01 Open wallet automatically  (Must, R2, 3 pts)
As a customer, I want my MUR wallet to be opened when my onboarding is approved so that I can add money and use it without extra steps.
Acceptance:
  - Created only when WALLET_ENABLED is on for my segment
  - one MUR wallet per customer
  - wallet terms accepted once and recorded with version and timestamp
  - wallet linked to a WALLET_CUSTOMER ledger sub-account
Note: Licensing and safeguarding rules to be confirmed by compliance (open question 13)

### E17-US02 View wallet balance and holds  (Must, R2, 2 pts)
As a customer, I want to see my posted balance, held amount and available balance so that I know how much I can spend or withdraw.
Acceptance:
  - Available = posted minus held
  - balances read from the ledger, never from a cached copy alone
  - held items listed with reason and expected release

### E17-US03 Top up wallet by card  (Must, R2, 5 pts)
As a customer, I want to add money to my wallet with a saved card so that I can pay for transfers instantly later.
Acceptance:
  - Fee and tax shown before confirm
  - step-up authentication
  - 3-D Secure via the PSP
  - wallet credited only when the pay-in is confirmed
  - min/max top-up, daily and monthly limits and maximum balance enforced
  - card-funded amount flagged for the withdrawal holding period

### E17-US04 Top up wallet by bank transfer or MauCAS  (Must, R2, 5 pts)
As a customer, I want to add money from my bank account or by MauCAS so that I can fund the wallet without a card.
Acceptance:
  - Works in both MauCAS modes
  - unique reference per top-up for matching
  - credited on callback or statement match
  - duplicate callbacks ignored
  - limits enforced

### E17-US05 Pay a transfer from the wallet  (Must, R2, 8 pts)
As a customer, I want to choose Wallet as the payment type when I send money so that my transfer starts immediately.
Acceptance:
  - Available balance checked at confirmation
  - debit, transfer creation and outbox event in one database transaction
  - pay-in confirmed instantly
  - idempotency key prevents double debit
  - posting: Dr WALLET_CUSTOMER A; Cr PAYOUT_IN_TRANSIT P, FEE_REVENUE F, TAX_PAYABLE T

### E17-US06 Handle insufficient wallet balance  (Should, R2, 3 pts)
As a customer, I want to be told when my balance is too low and to top up the difference or pick another method so that I can still complete my transfer.
Acceptance:
  - Shortfall amount shown
  - top-up of the difference returns to the same transfer review
  - quote expiry still applies

### E17-US07 Withdraw to my own bank account  (Must, R2, 8 pts)
As a customer, I want to move money from my wallet back to my bank account so that I can get my money out at any time.
Acceptance:
  - Only verified accounts in my own name
  - fee shown; step-up authentication
  - amount held at request
  - paid by bank transfer or MauCAS through a payout port
  - statuses REQUESTED, IN_REVIEW, SENT, PAID, FAILED, RETURNED
  - failure or return releases the hold and reverses postings
  - unknown result is queried before any retry

### E17-US08 Manage withdrawal accounts with cooling-off  (Must, R2, 3 pts)
As a customer, I want to add and remove the bank accounts I can withdraw to so that my money only goes to accounts I control.
Acceptance:
  - Name match with my profile
  - new account usable only after the cooling-off period (parameter)
  - change notified by push and email
  - four-eyes override in back-office only

### E17-US09 Refund to wallet  (Should, R2, 5 pts)
As a customer, I want refunds of failed transfers to go to my wallet where policy allows so that I can resend quickly.
Acceptance:
  - Transfers paid from the wallet always refund to the wallet
  - other payment types follow the refund policy parameter
  - fee and tax refund per policy
  - ledger reversal links to the original posting

### E17-US10 Wallet statement and history  (Must, R2, 3 pts)
As a customer, I want to see all wallet movements and download a monthly statement so that I can track my money.
Acceptance:
  - Filters by date and type
  - opening and closing balances tie to the ledger
  - PDF statement with masked data

### E17-US11 Wallet limits and fees as parameters  (Must, R2, 5 pts)
As a developer, I want to configure wallet limits, fees, holding period, cooling-off and dormancy in the parametrisation menu so that the business can change them without code.
Acceptance:
  - WALLET_ENABLED switch (default off)
  - limits by KYC level
  - fees by payment type and channel
  - maker-checker and versioning apply
  - no wallet value hard-coded

### E17-US12 Wallet ledger accounts and posting templates  (Must, R2, 8 pts)
As a finance analyst, I want wallet movements to be posted as balanced double-entry lines so that wallet money is always accounted for.
Acceptance:
  - WALLET_CUSTOMER liability sub-account per customer
  - WITHDRAWAL_IN_TRANSIT and SETTLEMENT_BANK accounts
  - templates for top-up, pay from wallet, withdrawal, withdrawal paid, reversal and refund
  - tax line always present, zero when tax is disabled
  - property tests keep every currency balanced

### E17-US13 Holds and concurrency control  (Must, R2, 5 pts)
As a developer, I want wallet debits to lock the wallet and check the available balance atomically so that the same money can never be spent twice.
Acceptance:
  - Row lock or version check on the wallet balance during debit
  - holds with type, reference, amount and expiry
  - expired holds released by a scheduled job
  - concurrent debit test passes with no negative balance

### E17-US14 Daily safeguarding reconciliation  (Must, R2, 5 pts)
As a finance analyst, I want total wallet balances compared with the safeguarding bank account every day so that any shortfall is caught at once.
Acceptance:
  - Sum of WALLET_CUSTOMER balances vs bank statement balance
  - differences stored as breaks with ageing
  - critical alert on shortfall
  - report exportable

### E17-US15 Wallet monitoring rules  (Must, R2, 5 pts)
As a compliance officer, I want rules that flag unusual wallet use so that wallet misuse is detected early.
Acceptance:
  - Top-up then withdrawal without transfers
  - rapid top-up and withdrawal cycles
  - top-ups from many different cards
  - hits create compliance cases
  - thresholds are parameters

### E17-US16 Back-office wallet 360, freeze and adjustments  (Must, R2, 5 pts)
As a support agent, I want to view a wallet, freeze or unfreeze it and request manual adjustments so that I can resolve issues safely.
Acceptance:
  - Balance, holds and movements visible with masked data
  - freeze blocks debits
  - adjustments need four-eyes approval and a reason
  - adjustments are new ledger entries, never edits
  - all actions audited

### E17-US17 Dormancy and wallet closure  (Could, R2, 3 pts)
As a customer, I want to close my wallet and receive my remaining balance so that I can leave the service cleanly.
Acceptance:
  - Dormant status after the dormancy period
  - reactivation needs step-up authentication
  - closure pays the balance to a verified own account first
  - closed wallet cannot be used

### E17-US18 Wallet reports  (Should, R2, 3 pts)
As a finance analyst, I want reports on wallet balances, top-ups, withdrawals and dormant wallets so that finance and compliance can monitor stored value.
Acceptance:
  - Balances by customer and segment
  - movements by type and period
  - ties to the ledger
  - role-based access, masked data, CSV/XLSX export

### E17-US19 Wallet notifications  (Should, R2, 2 pts)
As a customer, I want to be notified when a top-up is credited or a withdrawal is paid or fails so that I always know where my money is.
Acceptance:
  - Push and in-app messages
  - templates in several languages
  - no full account numbers in messages
