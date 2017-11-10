package

_RECORD_LIMIT = 10;

public class PendingList(){

	byte[] record = new byte[18]; 
	ArrayList<byte[]> pendingList = new ArrayList<byte[]>();

	public void removeRecord(byte[] record)
	{
		Iterator<byte> recordIt = pendingList.iterator();
        while (recordIt.hasNext()) {
			byte[] slice = Arrays.copyOfRange(recordIt.next(), 0, 15);
            if (slice == record) {
                recordIt.remove();
                // If you know it's unique, you could `break;` here
            }
        }
	};

	public void updateRecordStatus(byte[] record, byte status)
	{
		Iterator<byte> recordIt = pendingList.iterator();
   	    while (recordIt.hasNext()) {
			byte[] slice = Arrays.copyOfRange(recordIt.next(), 0, 15);
       	      if (slice == record) {
           	      recordIt[16] = status;
       	          // If you know it's unique, you could `break;` here
   	          }
         }
	};

	public void updateRecordNumOfReq(byte[] record)
	{
		Iterator<byte> recordIt = pendingList.iterator();
   	    while (recordIt.hasNext()) {
			byte[] slice = Arrays.copyOfRange(recordIt.next(), 0, 15);
       	      if (slice == record) {
           	      recordIt[17] += 1;
       	          // If you know it's unique, you could `break;` here
   	          }
         }
	};
};

