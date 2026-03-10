const express = require("express")
const crypto = require("crypto")
const qs = require("qs")
const cors = require("cors")

const app = express()

app.use(cors())
app.use(express.json())

const tmnCode = "JMJVT9AP"
const secretKey = "PW6HUCY3MNVTALCF7Z8ESU6O06KXPKSL"
const vnpUrl = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html"
const returnUrl = "http://localhost:5173/payment-return"

function sortObject(obj) {
  let sorted = {}
  let str = []
  let key

  for (key in obj) {
    if (obj.hasOwnProperty(key)) {
      str.push(encodeURIComponent(key))
    }
  }

  str.sort()

  for (key = 0; key < str.length; key++) {
    sorted[str[key]] = encodeURIComponent(obj[str[key]]).replace(/%20/g, "+")
  }

  return sorted
}

app.post("/create-payment", (req, res) => {

  const amount = req.body.amount

  const date = new Date()

  const createDate =
    date.getFullYear() +
    ("0" + (date.getMonth() + 1)).slice(-2) +
    ("0" + date.getDate()).slice(-2) +
    ("0" + date.getHours()).slice(-2) +
    ("0" + date.getMinutes()).slice(-2) +
    ("0" + date.getSeconds()).slice(-2)

  let params = {
    vnp_Version: "2.1.0",
    vnp_Command: "pay",
    vnp_TmnCode: tmnCode,
    vnp_Amount: amount * 100,
    vnp_CurrCode: "VND",
    vnp_TxnRef: Date.now(),
    vnp_OrderInfo: "Thanh toan don hang",
    vnp_OrderType: "other",
    vnp_Locale: "vn",
    vnp_ReturnUrl: returnUrl,
    vnp_IpAddr: "127.0.0.1",
    vnp_CreateDate: createDate
  }

  params = sortObject(params)

  const signData = qs.stringify(params, { encode: false })

  const hmac = crypto.createHmac("sha512", secretKey)
  const signed = hmac.update(signData).digest("hex")

  params["vnp_SecureHash"] = signed

  const paymentUrl = vnpUrl + "?" + qs.stringify(params, { encode: false })

  res.json({ url: paymentUrl })
})

app.listen(3000, () => {
  console.log("VNPay backend running on port 3000")
})