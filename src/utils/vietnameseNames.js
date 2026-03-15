const EMAIL_NAME_MAP = {
  "admin@dirtywave.com": "Nguyen Duc Anh",
  "admin2@dirtywave.com": "Tran Minh Quan",
  "employee1@dirtywave.com": "Nguyen Minh Khoa",
  "employee2@dirtywave.com": "Tran Quoc Bao",
  "employee3@dirtywave.com": "Le Hoang Nam",
  "employee4@dirtywave.com": "Pham Gia Huy",
  "employee5@dirtywave.com": "Vo Thanh Dat",
  "employee6@dirtywave.com": "Bui Tuan Anh",
  "employee7@dirtywave.com": "Dang Ngoc Son",
  "employee8@dirtywave.com": "Hoang Minh Tri",
  "employee9@dirtywave.com": "Doan Quang Hieu",
  "employee10@dirtywave.com": "Nguyen Thi Mai Anh",
  "employee11@dirtywave.com": "Tran Khanh Linh",
  "employee12@dirtywave.com": "Le Thu Thao",
  "customer1@dirtywave.com": "Nguyen Hai Nam",
  "customer2@dirtywave.com": "Tran Bao Ngoc",
  "customer3@dirtywave.com": "Le Duc Manh",
  "customer4@dirtywave.com": "Pham Thu Ha",
  "customer5@dirtywave.com": "Vo Hoang Long",
  "customer6@dirtywave.com": "Bui Khanh Vy",
  "customer7@dirtywave.com": "Dang Tuan Kiet",
  "customer8@dirtywave.com": "Hoang Gia Han",
  "customer9@dirtywave.com": "Doan Minh Tam",
  "customer10@dirtywave.com": "Nguyen Phuong Linh",
  "customer11@dirtywave.com": "Tran Ngoc Anh",
  "customer12@dirtywave.com": "Le Quang Huy"
}

const EMPLOYEE_NUMBER_MAP = {
  1: "Nguyen Minh Khoa",
  2: "Tran Quoc Bao",
  3: "Le Hoang Nam",
  4: "Pham Gia Huy",
  5: "Vo Thanh Dat",
  6: "Bui Tuan Anh",
  7: "Dang Ngoc Son",
  8: "Hoang Minh Tri",
  9: "Doan Quang Hieu",
  10: "Nguyen Thi Mai Anh",
  11: "Tran Khanh Linh",
  12: "Le Thu Thao"
}

const CUSTOMER_NUMBER_MAP = {
  1: "Nguyen Hai Nam",
  2: "Tran Bao Ngoc",
  3: "Le Duc Manh",
  4: "Pham Thu Ha",
  5: "Vo Hoang Long",
  6: "Bui Khanh Vy",
  7: "Dang Tuan Kiet",
  8: "Hoang Gia Han",
  9: "Doan Minh Tam",
  10: "Nguyen Phuong Linh",
  11: "Tran Ngoc Anh",
  12: "Le Quang Huy"
}

const GENERIC_NAME_PATTERNS = [
  /^quan tri vien$/i,
  /^khach hang\s*\d+$/i,
  /^nhan vien\s*\d+$/i,
  /^admin$/i,
  /^customer\s*\d+$/i,
  /^employee\s*\d+$/i
]

export const isGenericVietnameseName = (value) => {
  const name = String(value || "").trim()
  if (!name) return true
  return GENERIC_NAME_PATTERNS.some((pattern) => pattern.test(name))
}

export const getVietnameseNameByEmail = (email) => {
  const key = String(email || "").trim().toLowerCase()
  if (EMAIL_NAME_MAP[key]) return EMAIL_NAME_MAP[key]

  const employeeMatch = key.match(/^employee0*([0-9]+)@dirtywave\.com$/)
  if (employeeMatch) {
    const index = Number(employeeMatch[1])
    return EMPLOYEE_NUMBER_MAP[index] || ""
  }

  const customerMatch = key.match(/^customer0*([0-9]+)@dirtywave\.com$/)
  if (customerMatch) {
    const index = Number(customerMatch[1])
    return CUSTOMER_NUMBER_MAP[index] || ""
  }

  return ""
}
